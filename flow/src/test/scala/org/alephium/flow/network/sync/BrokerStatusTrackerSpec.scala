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

import akka.actor.ActorRef
import akka.testkit.TestProbe

import org.oxygenium.flow.OxygeniumFlowActorSpec
import org.oxygenium.flow.setting.NetworkSetting
import org.oxygenium.protocol.Generators
import org.oxygenium.util.ActorRefT

class BrokerStatusTrackerSpec extends OxygeniumFlowActorSpec with Generators {
  val brokerInfo = brokerInfoGen.sample.get

  trait Fixture extends BrokerStatusTracker {
    def networkSetting: NetworkSetting = config.network

    def addNewBroker(): ActorRef = {
      val broker = TestProbe().ref
      brokerInfos += ActorRefT(broker) -> brokerInfo
      broker
    }
  }

  it should "sample the right size" in new Fixture {
    networkSetting.syncPeerSampleSize is 3

    (1 until 4).foreach(_ => addNewBroker())
    samplePeersSize() is 1
    samplePeers().toSeq.toMap.size is 1
    (4 until 9).foreach(_ => addNewBroker())
    samplePeersSize() is 2
    samplePeers().toSeq.toMap.size is 2
    (9 until 1024).foreach(_ => addNewBroker())
    samplePeersSize() is 3
    samplePeers().toSeq.toMap.size is 3
  }
}
