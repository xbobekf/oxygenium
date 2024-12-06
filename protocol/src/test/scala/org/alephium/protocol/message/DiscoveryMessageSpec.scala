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

package org.alephium.protocol.message

import java.net.InetSocketAddress

import org.alephium.macros.EnumerationMacros
import org.alephium.protocol.{DiscoveryVersion, SignatureSchema}
import org.alephium.protocol.config.{BrokerConfig, DiscoveryConfig, NetworkConfigFixture}
import org.alephium.protocol.model.{BrokerInfo, CliqueId}
import org.alephium.serde._
import org.alephium.util.{AlephiumSpec, AVector, Duration}

class DiscoveryMessageSpec extends AlephiumSpec with NetworkConfigFixture.Default {
  import DiscoveryMessage.{Code, Header}

  implicit val ordering: Ordering[Code[_]] = Ordering.by(Code.toInt(_))

  it should "index all codes" in {
    val codes = EnumerationMacros.sealedInstancesOf[Code[_]]
    Code.values is AVector.from(codes)
  }

  // TODO: clean code
  trait DiscoveryConfigFixture { self =>
    def groups: Int
    def brokerNum: Int
    def groupNumPerBroker: Int
    def publicAddress: InetSocketAddress = new InetSocketAddress(1)
    def brokerInfo: BrokerInfo
    def isCoordinator: Boolean

    val (discoveryPrivateKey, discoveryPublicKey) = SignatureSchema.generatePriPub()

    implicit val brokerConfig: BrokerConfig = new BrokerConfig {
      override def brokerId: Int = 1

      override def brokerNum: Int = self.groups

      override def groups: Int = self.groups
    }

    implicit val discoveryConfig: DiscoveryConfig = new DiscoveryConfig {
      val scanFrequency: Duration          = Duration.ofSecondsUnsafe(1)
      val scanFastFrequency: Duration      = Duration.ofSecondsUnsafe(1)
      val fastScanPeriod: Duration         = Duration.ofMinutesUnsafe(1)
      val initialDiscoveryPeriod: Duration = Duration.ofSecondsUnsafe(30)
      val neighborsPerGroup: Int           = 1
      val maxCliqueFromSameIp: Int         = 2
    }
  }

  it should "serialize/deserialize the Header when version compatible" in {
    val header = Header(DiscoveryVersion.currentDiscoveryVersion)
    val bytes  = serialize(header)
    deserialize[Header](bytes) isE header
  }

  it should "deserialize failed when version not compatible" in {
    val invalidVersion = DiscoveryVersion(DiscoveryVersion.currentDiscoveryVersion.value + 1)
    val bytes          = serialize(Header(invalidVersion))
    deserialize[Header](bytes).leftValue is a[SerdeError]
  }

  it should "support serde for all message types" in new DiscoveryConfigFixture
    with DiscoveryMessageGenerators {
    def groups: Int            = 4
    def brokerNum: Int         = 4
    def groupNumPerBroker: Int = 1
    def brokerInfo: BrokerInfo =
      BrokerInfo.unsafe(CliqueId.generate, 0, groupNumPerBroker, publicAddress)
    def isCoordinator: Boolean = true

    val peerFixture = new DiscoveryConfigFixture {
      def groups: Int            = 4
      def brokerNum: Int         = 4
      def groupNumPerBroker: Int = 1
      def brokerInfo: BrokerInfo =
        BrokerInfo.unsafe(CliqueId.generate, 0, groupNumPerBroker, publicAddress)
      def isCoordinator: Boolean = false
    }
    forAll(messageGen(peerFixture.brokerConfig)) { msg =>
      val bytes = DiscoveryMessage.serialize(
        msg,
        discoveryPrivateKey
      )
      val value = DiscoveryMessage.deserialize(bytes)
      msg is value.rightValue
    }
  }
}
