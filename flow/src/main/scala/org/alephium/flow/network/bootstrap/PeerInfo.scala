// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
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

package org.alephium.flow.network.bootstrap

import java.net.InetSocketAddress

import org.alephium.flow.setting.{Configs, NetworkSetting}
import org.alephium.protocol.SafeSerdeImpl
import org.alephium.protocol.config.{BrokerConfig, GroupConfig}
import org.alephium.protocol.model.BrokerInfo
import org.alephium.serde._

final case class PeerInfo private (
    id: Int,
    groupNumPerBroker: Int,
    externalAddress: Option[InetSocketAddress],
    internalAddress: InetSocketAddress,
    restPort: Int,
    wsPort: Int,
    minerApiPort: Int
)

object PeerInfo extends SafeSerdeImpl[PeerInfo, GroupConfig] {
  def unsafe(
      id: Int,
      groupNumPerBroker: Int,
      publicAddress: Option[InetSocketAddress],
      privateAddress: InetSocketAddress,
      restPort: Int,
      wsPort: Int,
      minerApiPort: Int
  ): PeerInfo =
    new PeerInfo(
      id,
      groupNumPerBroker,
      publicAddress,
      privateAddress,
      restPort,
      wsPort,
      minerApiPort
    )

  val unsafeSerde: Serde[PeerInfo] =
    Serde.forProduct7(
      unsafe,
      t =>
        (
          t.id,
          t.groupNumPerBroker,
          t.externalAddress,
          t.internalAddress,
          t.restPort,
          t.wsPort,
          t.minerApiPort
        )
    )

  override def validate(info: PeerInfo)(implicit config: GroupConfig): Either[String, Unit] = {
    for {
      _ <- check(info.groupNumPerBroker)
      _ <- BrokerInfo.validate(info.id, config.groups / info.groupNumPerBroker)
      _ <- info.externalAddress.fold[Either[String, Unit]](Right(()))(address =>
        Configs.validatePort(address.getPort)
      )
      _ <- Configs.validatePort(info.restPort)
      _ <- Configs.validatePort(info.wsPort)
    } yield ()
  }

  private def check(groupNumPerBroker: Int)(implicit config: GroupConfig): Either[String, Unit] = {
    if (
      (groupNumPerBroker <= 0) ||
      (groupNumPerBroker > config.groups) ||
      (config.groups % groupNumPerBroker != 0)
    ) {
      Left(s"PeerInfo: invalid groupNumPerBroker: $groupNumPerBroker")
    } else {
      Right(())
    }
  }

  def self(implicit brokerConfig: BrokerConfig, networkSetting: NetworkSetting): PeerInfo = {
    new PeerInfo(
      brokerConfig.brokerId,
      brokerConfig.groupNumPerBroker,
      networkSetting.externalAddressInferred,
      networkSetting.internalAddress,
      networkSetting.restPort,
      networkSetting.wsPort,
      networkSetting.minerApiPort
    )
  }
}
