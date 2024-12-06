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

package org.oxygenium.flow.core

import org.oxygenium.flow.io.Storages
import org.oxygenium.flow.setting.{OxygeniumConfig, ConsensusSettings, MemPoolSetting, NetworkSetting}
import org.oxygenium.io.IOResult
import org.oxygenium.protocol.config.BrokerConfig
import org.oxygenium.protocol.model._
import org.oxygenium.protocol.vm.{LogConfig, NodeIndexesConfig, WorldState}
import org.oxygenium.util.AVector

trait EmptyBlockFlow extends BlockFlow {
  def storages: Storages
  implicit def config: OxygeniumConfig

  implicit lazy val brokerConfig: BrokerConfig           = config.broker
  implicit lazy val consensusConfigs: ConsensusSettings  = config.consensus
  implicit lazy val networkConfig: NetworkSetting        = config.network
  implicit lazy val mempoolSetting: MemPoolSetting       = config.mempool
  implicit lazy val logConfig: LogConfig                 = config.node.eventLogConfig
  implicit lazy val nodeIndexesConfig: NodeIndexesConfig = config.node.indexesConfig

  lazy val genesisBlocks: AVector[AVector[Block]] = config.genesisBlocks

  lazy val blockchainWithStateBuilder: (Block, BlockFlow.WorldStateUpdater) => BlockChainWithState =
    BlockChainWithState.fromGenesisUnsafe(storages)
  lazy val blockchainBuilder: Block => BlockChain =
    BlockChain.fromGenesisUnsafe(storages)
  lazy val blockheaderChainBuilder: BlockHeader => BlockHeaderChain =
    BlockHeaderChain.fromGenesisUnsafe(storages)

  def getAllTips: AVector[BlockHash]                                                           = ???
  def getBestTipUnsafe(): BlockHash                                                            = ???
  def calBestDepsUnsafe(group: GroupIndex): BlockDeps                                          = ???
  def updateBestDeps(): IOResult[Unit]                                                         = ???
  def updateBestDepsUnsafe(): Unit                                                             = ???
  def getBestIntraGroupTip(): BlockHash                                                        = ???
  def add(block: Block, worldStateOpt: Option[WorldState.Cached]): IOResult[Unit]              = ???
  def add(header: BlockHeader): IOResult[Unit]                                                 = ???
  def addAndUpdateView(block: Block, worldStateOpt: Option[WorldState.Cached]): IOResult[Unit] = ???
  def addAndUpdateView(header: BlockHeader): IOResult[Unit]                                    = ???
}
