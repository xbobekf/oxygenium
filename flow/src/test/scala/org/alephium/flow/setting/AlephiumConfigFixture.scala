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

package org.oxygenium.flow.setting

import scala.jdk.CollectionConverters._

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}

import org.oxygenium.flow.setting._
import org.oxygenium.protocol.{ALPH, PrivateKey, PublicKey}
import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.protocol.model.{Address, GroupIndex}
import org.oxygenium.protocol.vm.{LogConfig, NodeIndexesConfig}
import org.oxygenium.util.{AVector, Duration, Env, Number, U256}

trait OxygeniumConfigFixture extends RandomPortsConfigFixture {

  val configValues: Map[String, Any] = Map.empty

  val genesisBalance: U256 = ALPH.alph(Number.million)

  lazy val env      = Env.resolve()
  lazy val rootPath = Platform.getRootPath(env)

  def buildNewConfig() = {
    val predefined = ConfigFactory
      .parseMap(
        (configPortsValues ++ configValues).view
          .mapValues {
            case value: AVector[_] =>
              ConfigValueFactory.fromIterable(value.toIterable.asJava)
            case value =>
              ConfigValueFactory.fromAnyRef(value)
          }
          .toMap
          .asJava
      )
    Configs.parseConfig(Env.currentEnv, rootPath, overwrite = true, predefined)
  }
  lazy val newConfig = buildNewConfig()

  lazy val groups0 = newConfig.getInt("oxygenium.broker.groups")

  lazy val groupConfig: GroupConfig = new GroupConfig { override def groups: Int = groups0 }

  lazy val genesisKeys =
    AVector.tabulate[(PrivateKey, PublicKey, U256)](groups0) { i =>
      val groupIndex              = GroupIndex.unsafe(i)(groupConfig)
      val (privateKey, publicKey) = groupIndex.generateKey(groupConfig)
      (privateKey, publicKey, genesisBalance)
    }

  implicit lazy val config: OxygeniumConfig = {
    val tmp = OxygeniumConfig
      .load(newConfig)

    val allocations = genesisKeys.map { case (_, pubKey, amount) =>
      Allocation(Address.p2pkh(pubKey), Allocation.Amount(amount), Duration.zero)
    }
    tmp.copy(genesis = GenesisSetting(allocations))
  }

  implicit lazy val brokerConfig: BrokerSetting          = config.broker
  implicit lazy val consensusConfigs: ConsensusSettings  = config.consensus
  implicit lazy val networkConfig: NetworkSetting        = config.network
  implicit lazy val discoverySetting: DiscoverySetting   = config.discovery
  implicit lazy val memPoolSetting: MemPoolSetting       = config.mempool
  implicit lazy val miningSetting: MiningSetting         = config.mining
  implicit lazy val nodeSetting: NodeSetting             = config.node
  implicit lazy val logConfig: LogConfig                 = config.node.eventLogConfig
  implicit lazy val nodeIndexesConfig: NodeIndexesConfig = config.node.indexesConfig
}
