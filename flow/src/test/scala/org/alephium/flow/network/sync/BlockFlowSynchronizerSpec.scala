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

import akka.testkit.{TestActorRef, TestProbe}

import org.oxygenium.flow.FlowFixture
import org.oxygenium.flow.handler.TestUtils
import org.oxygenium.flow.network.InterCliqueManager
import org.oxygenium.flow.network.broker.{BrokerHandler, InboundConnection}
import org.oxygenium.protocol.Generators
import org.oxygenium.protocol.model.BlockHash
import org.oxygenium.util.{OxygeniumActorSpec, AVector}

class BlockFlowSynchronizerSpec extends OxygeniumActorSpec {
  trait Fixture extends FlowFixture with Generators {
    val (allHandlers, _) = TestUtils.createAllHandlersProbe
    val blockFlowSynchronizer = TestActorRef[BlockFlowSynchronizer](
      BlockFlowSynchronizer.props(blockFlow, allHandlers)
    )
    val blockFlowSynchronizerActor = blockFlowSynchronizer.underlyingActor
  }

  it should "add/remove brokers" in new Fixture {
    blockFlowSynchronizerActor.brokerInfos.isEmpty is true

    val probe  = TestProbe()
    val broker = brokerInfoGen.sample.get
    probe.send(
      blockFlowSynchronizer,
      InterCliqueManager.HandShaked(probe.ref, broker, InboundConnection, "")
    )
    eventually(blockFlowSynchronizerActor.brokerInfos.toMap.contains(probe.ref) is true)

    system.stop(probe.ref)
    eventually(blockFlowSynchronizerActor.brokerInfos.isEmpty is true)
  }

  it should "handle block announcement" in new Fixture {
    val broker     = TestProbe()
    val brokerInfo = brokerInfoGen.sample.get
    val blockHash  = BlockHash.generate

    broker.send(
      blockFlowSynchronizer,
      InterCliqueManager.HandShaked(broker.ref, brokerInfo, InboundConnection, "")
    )
    eventually(blockFlowSynchronizerActor.brokerInfos.toMap.contains(broker.ref) is true)
    broker.send(blockFlowSynchronizer, BlockFlowSynchronizer.BlockAnnouncement(blockHash))
    broker.expectMsg(BrokerHandler.DownloadBlocks(AVector(blockHash)))
    eventually(blockFlowSynchronizerActor.fetching.states.contains(blockHash) is true)
  }
}
