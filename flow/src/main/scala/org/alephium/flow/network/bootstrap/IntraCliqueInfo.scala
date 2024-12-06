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

import org.alephium.protocol.{PrivateKey, SafeSerdeImpl}
import org.alephium.protocol.config.GroupConfig
import org.alephium.protocol.model.{CliqueId, CliqueInfo}
import org.alephium.serde._
import org.alephium.util.AVector

final case class IntraCliqueInfo private (
    id: CliqueId,
    peers: AVector[PeerInfo],
    groupNumPerBroker: Int,
    priKey: PrivateKey
) {
  def cliqueInfo: CliqueInfo = {
    CliqueInfo.unsafe(
      id,
      peers.map(_.externalAddress),
      peers.map(_.internalAddress),
      groupNumPerBroker,
      priKey
    )
  }
}

object IntraCliqueInfo extends SafeSerdeImpl[IntraCliqueInfo, GroupConfig] {
  def unsafe(
      id: CliqueId,
      peers: AVector[PeerInfo],
      groupNumPerBroker: Int,
      priKey: PrivateKey
  ): IntraCliqueInfo = {
    new IntraCliqueInfo(id, peers, groupNumPerBroker, priKey)
  }

  implicit private val peerSerde: Serde[PeerInfo]           = PeerInfo.unsafeSerde
  implicit private val peersSerde: Serde[AVector[PeerInfo]] = avectorSerde[PeerInfo]
  override val unsafeSerde: Serde[IntraCliqueInfo] =
    Serde.forProduct4(unsafe, t => (t.id, t.peers, t.groupNumPerBroker, t.priKey))

  override def validate(
      info: IntraCliqueInfo
  )(implicit config: GroupConfig): Either[String, Unit] = {
    for {
      _ <- checkGroups(info)
      _ <- checkPeers(info)
    } yield ()
  }

  private def checkGroups(
      info: IntraCliqueInfo
  )(implicit config: GroupConfig): Either[String, Unit] = {
    if (info.peers.length * info.groupNumPerBroker != config.groups) {
      Left(s"invalid groups: $info")
    } else {
      Right(())
    }
  }

  private def checkPeers(
      info: IntraCliqueInfo
  )(implicit config: GroupConfig): Either[String, Unit] = {
    info.peers.foreachWithIndexE { (peer, index) =>
      if (peer.id != index) {
        Left(s"invalid index: $peer")
      } else {
        PeerInfo.validate(peer)
      }
    }
  }
}
