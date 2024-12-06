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

package org.oxygenium.flow.network.broker

import java.net.InetSocketAddress

import akka.actor.Props
import akka.io.Tcp
import akka.testkit.{TestActorRef, TestProbe}

import org.oxygenium.flow.core.BlockFlow
import org.oxygenium.flow.handler.AllHandlers
import org.oxygenium.flow.model.DataOrigin
import org.oxygenium.flow.network.{CliqueManager, TcpController}
import org.oxygenium.flow.network.sync.BlockFlowSynchronizer
import org.oxygenium.flow.setting.{OxygeniumConfigFixture, NetworkSetting}
import org.oxygenium.protocol.Generators
import org.oxygenium.protocol.config.BrokerConfig
import org.oxygenium.protocol.model.{BrokerInfo, CliqueInfo}
import org.oxygenium.util.{ActorRefT, OxygeniumActorSpec}

class OutboundBrokerHandlerSpec extends OxygeniumActorSpec {
  it should "retry to connect if the connection failed" in new Fixture {
    listener.expectMsg(TcpController.ConnectTo(remoteAddress, ActorRefT(brokerHandler)))
    brokerHandler ! Tcp.CommandFailed(Tcp.Connect(remoteAddress))
    listener.expectMsg(TcpController.ConnectTo(remoteAddress, ActorRefT(brokerHandler)))
    val connection = TestProbe()
    brokerHandler.underlyingActor.connection isnot ActorRefT[Tcp.Command](connection.ref)
    connection.send(brokerHandler, Tcp.Connected(remoteAddress, localAddress))
    brokerHandler.underlyingActor.connection is ActorRefT[Tcp.Command](connection.ref)
  }

  it should "stop when connection retry failed" in new Fixture {
    override val configValues: Map[String, Any] = Map(
      "oxygenium.network.backoff-base-delay" -> "10 milli",
      "oxygenium.network.backoff-max-delay"  -> "100 milli"
    )

    watch(brokerHandler)
    (0 to BackoffStrategy.maxRetry).foreach { _ =>
      listener.expectMsg(TcpController.ConnectTo(remoteAddress, ActorRefT(brokerHandler)))
      brokerHandler ! Tcp.CommandFailed(Tcp.Connect(remoteAddress))
    }
    expectTerminated(brokerHandler)
  }

  trait Fixture extends OxygeniumConfigFixture {
    val listener = TestProbe()
    system.eventStream.subscribe(listener.ref, classOf[TcpController.ConnectTo])

    val localAddress  = Generators.socketAddressGen.sample.get
    val remoteAddress = Generators.socketAddressGen.sample.get
    lazy val brokerHandler = TestActorRef[TestOutboundBrokerHandler](
      TestOutboundBrokerHandler.props(remoteAddress)
    )
  }
}

object TestOutboundBrokerHandler {
  def props(
      remoteAddress: InetSocketAddress
  )(implicit brokerConfig: BrokerConfig, networkSetting: NetworkSetting): Props =
    Props(new TestOutboundBrokerHandler(remoteAddress))
}

class TestOutboundBrokerHandler(
    val remoteAddress: InetSocketAddress
)(implicit val brokerConfig: BrokerConfig, val networkSetting: NetworkSetting)
    extends OutboundBrokerHandler {
  override def selfCliqueInfo: CliqueInfo =
    Generators.cliqueInfoGen(1).sample.get
  override def exchanging: Receive                                             = exchangingCommon
  override def dataOrigin: DataOrigin                                          = ???
  override def allHandlers: AllHandlers                                        = ???
  override def blockflow: BlockFlow                                            = ???
  override def blockFlowSynchronizer: ActorRefT[BlockFlowSynchronizer.Command] = ???
  override def cliqueManager: ActorRefT[CliqueManager.Command]                 = ???

  def handleHandshakeInfo(_remoteBrokerInfo: BrokerInfo, clientInfo: String): Unit = {
    remoteBrokerInfo = _remoteBrokerInfo
  }
}
