// Copyright 2018 The Oxygenium Authors
// This file is part of the oxygenium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

package org.oxygenium.flow.network.sync

import java.net.InetSocketAddress

import akka.actor.{Props, Terminated}

import org.oxygenium.flow.core.BlockFlow
import org.oxygenium.flow.handler.{AllHandlers, FlowHandler, IOBaseActor}
import org.oxygenium.flow.network._
import org.oxygenium.flow.network.broker.BrokerHandler
import org.oxygenium.flow.setting.NetworkSetting
import org.oxygenium.protocol.config.BrokerConfig
import org.oxygenium.protocol.model.BlockHash
import org.oxygenium.util.{ActorRefT, AVector}
import org.oxygenium.util.EventStream.Subscriber

object BlockFlowSynchronizer {
  def props(blockflow: BlockFlow, allHandlers: AllHandlers)(implicit
      networkSetting: NetworkSetting,
      brokerConfig: BrokerConfig
  ): Props =
    Props(new BlockFlowSynchronizer(blockflow, allHandlers))

  sealed trait Command
  case object Sync                                                      extends Command
  final case class SyncInventories(hashes: AVector[AVector[BlockHash]]) extends Command
  final case class BlockFinalized(hash: BlockHash)                      extends Command
  case object CleanDownloading                                          extends Command
  final case class BlockAnnouncement(hash: BlockHash)                   extends Command
}

class BlockFlowSynchronizer(val blockflow: BlockFlow, val allHandlers: AllHandlers)(implicit
    val networkSetting: NetworkSetting,
    val brokerConfig: BrokerConfig
) extends IOBaseActor
    with Subscriber
    with DownloadTracker
    with BlockFetcher
    with BrokerStatusTracker
    with InterCliqueManager.NodeSyncStatus {
  import BlockFlowSynchronizer._

  override def preStart(): Unit = {
    super.preStart()
    schedule(self, CleanDownloading, networkSetting.syncCleanupFrequency)
    scheduleSync()
    subscribeEvent(self, classOf[InterCliqueManager.HandShaked])
  }

  override def receive: Receive = handle orElse updateNodeSyncStatus

  def handle: Receive = {
    case InterCliqueManager.HandShaked(broker, remoteBrokerInfo, _, _) =>
      log.debug(s"HandShaked with ${remoteBrokerInfo.address}")
      context.watch(broker.ref)
      brokerInfos += broker -> remoteBrokerInfo
    case Sync =>
      if (brokerInfos.nonEmpty) {
        log.debug(s"Send sync requests to the network")
        allHandlers.flowHandler ! FlowHandler.GetSyncLocators
      }
      scheduleSync()
    case flowLocators: FlowHandler.SyncLocators =>
      samplePeers().foreach { case (actor, brokerInfo) =>
        actor ! BrokerHandler.SyncLocators(flowLocators.filerFor(brokerInfo))
      }
    case SyncInventories(hashes) => download(hashes)
    case BlockFinalized(hash)    => finalized(hash)
    case CleanDownloading        => cleanupSyncing(networkSetting.syncExpiryPeriod)
    case BlockAnnouncement(hash) => handleBlockAnnouncement(hash)
    case Terminated(actor) =>
      val broker = ActorRefT[BrokerHandler.Command](actor)
      log.debug(s"Connection to ${remoteAddress(broker)} is closing")
      brokerInfos.filterInPlace(_._1 != broker)
  }

  private def remoteAddress(broker: ActorRefT[BrokerHandler.Command]): InetSocketAddress = {
    val brokerIndex = brokerInfos.indexWhere(_._1 == broker)
    brokerInfos(brokerIndex)._2.address
  }

  def scheduleSync(): Unit = {
    val frequency =
      if (isNodeSynced) networkSetting.stableSyncFrequency else networkSetting.fastSyncFrequency
    scheduleOnce(self, Sync, frequency)
  }
}
