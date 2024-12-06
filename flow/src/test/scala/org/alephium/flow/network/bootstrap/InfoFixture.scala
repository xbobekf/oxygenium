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

import org.scalacheck.Gen

import org.alephium.flow.network.broker.MisbehaviorManager
import org.alephium.protocol.model.{CliqueInfo, ModelGenerators}
import org.alephium.util.TimeStamp

trait InfoFixture extends ModelGenerators {
  def intraCliqueInfoGen(cliqueInfoGen: Gen[CliqueInfo]): Gen[IntraCliqueInfo] = {
    for {
      info         <- cliqueInfoGen
      restPort     <- portGen
      wsPort       <- portGen
      minerApiPort <- portGen
    } yield {
      val peers = info.internalAddresses.mapWithIndex { (address, id) =>
        PeerInfo.unsafe(
          id,
          info.groupNumPerBroker,
          Some(address),
          address,
          restPort,
          wsPort,
          minerApiPort
        )
      }
      IntraCliqueInfo.unsafe(info.id, peers, info.groupNumPerBroker, info.priKey)
    }
  }

  def genBrokerPeers: Gen[MisbehaviorManager.Peers] = {
    for {
      info  <- cliqueInfoGen
      score <- Gen.choose(0, 42)
    } yield {
      val peers = info.internalAddresses.map { address =>
        MisbehaviorManager.Peer(
          address.getAddress,
          MisbehaviorManager.Penalty(score, TimeStamp.now())
        )
      }
      MisbehaviorManager.Peers(peers)
    }
  }

  def genIntraCliqueInfo(groupNumPerBroker: Int): IntraCliqueInfo =
    intraCliqueInfoGen(cliqueInfoGen(groupNumPerBroker)).sample.get

  def genIntraCliqueInfo: IntraCliqueInfo = {
    intraCliqueInfoGen(cliqueInfoGen).sample.get
  }
}
