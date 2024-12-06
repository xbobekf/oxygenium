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

package org.oxygenium.app

import java.io.File

import scala.jdk.CollectionConverters._

import com.typesafe.config.{ConfigException, ConfigFactory, ConfigValueFactory}

import org.oxygenium.flow.setting.{OxygeniumConfig, Configs, Platform}
import org.oxygenium.protocol.model.NetworkId
import org.oxygenium.util.{OxygeniumSpec, AVector, Env}

// scalastyle:off null
class ApiConfigSpec extends OxygeniumSpec {
  it should "load api config" in {
    val path   = getClass.getResource(s"/system_test.conf.tmpl").getPath
    val file   = new File(path)
    val config = ConfigFactory.parseFile(file)
    ApiConfig.load(config)
  }

  it should "load mainnet api config" in {
    val randomPath = Platform.getRootPath(Env.Test) // generate a random folder
    val prodConfig = Configs.parseConfig(Env.Prod, randomPath, false, ConfigFactory.empty())
    val alphConfig = OxygeniumConfig.load(prodConfig)
    val apiConfig  = ApiConfig.load(prodConfig)
    alphConfig.network.networkId is NetworkId.OxygeniumMainNet
    apiConfig.defaultUtxosLimit is 5000
  }

  behavior of "Api interface is 127.0.0.1"

  it should "use defined api key when key is provided" in new ApiKeyConfigFixture {
    override val apiKeyEnabled = true
    override val apiKey = AVector("74beb7e20967727763f3c88a1ef596e7b22049047cc6fa8ea27358b32c68377")

    apiConfig.apiKey.map(_.value) is apiKey
  }

  it should "use defined api key even if key is not enabled" in new ApiKeyConfigFixture {
    override val apiKeyEnabled = false
    override val apiKey = AVector(
      "74beb7e20967727763f3c88a1ef596e7b22049047cc6fa8ea27358b32c68377",
      "596e7b27e20967727763f3c88a1ef2049047cc6fa8ea27358b32c6837774beb"
    )

    apiConfig.apiKey.map(_.value) is apiKey
  }

  it should "not ask for api key even if key is enabled" in new ApiKeyConfigFixture {
    override def apiKeyEnabled: Boolean  = true
    override def apiKey: AVector[String] = null

    apiConfig.apiKey.headOption is None
  }

  it should "not ask for api key even if key is not enabled" in new ApiKeyConfigFixture {
    override def apiKeyEnabled: Boolean  = false
    override def apiKey: AVector[String] = null

    apiConfig.apiKey.headOption is None
  }

  behavior of "Api interface is not 127.0.0.1"

  it should "use defined api key when key is enabled" in new ApiKeyConfigFixture {
    override def interface     = "1.2.3.4"
    override val apiKeyEnabled = true
    override val apiKey = AVector(
      "74beb7e20967727763f3c88a1ef596e7b22049047cc6fa8ea27358b32c68377",
      "596e7b27e20967727763f3c88a1ef2049047cc6fa8ea27358b32c6837774beb"
    )

    apiConfig.apiKey.map(_.value) is apiKey
  }

  it should "use defined api key key is not enabled" in new ApiKeyConfigFixture {
    override def interface     = "1.2.3.4"
    override val apiKeyEnabled = false
    override val apiKey = AVector("74beb7e20967727763f3c88a1ef596e7b22049047cc6fa8ea27358b32c68377")

    apiConfig.apiKey.map(_.value) is apiKey
  }

  it should "ask for api key if key is enabled" in new ApiKeyConfigFixture {
    override def interface               = "1.2.3.4"
    override def apiKeyEnabled: Boolean  = true
    override def apiKey: AVector[String] = null

    assertThrows[ConfigException] {
      apiConfig
    }
  }

  it should "not ask for api key if key is not enabled" in new ApiKeyConfigFixture {
    override def interface               = "1.2.3.4"
    override def apiKeyEnabled: Boolean  = false
    override def apiKey: AVector[String] = null

    apiConfig.apiKey.headOption is None
  }

  trait ApiKeyConfigFixture {
    def interface: String = "127.0.0.1"
    def apiKey: AVector[String]
    def apiKeyEnabled: Boolean

    lazy val apiKeyValue = if (apiKey == null) {
      null
    } else {
      if (apiKey.length == 1) apiKey.head else apiKey
    }

    lazy val configValues: Map[String, Any] = Map(
      ("oxygenium.api.network-interface", interface),
      ("oxygenium.api.blockflow-fetch-max-age", "30 minutes"),
      ("oxygenium.api.ask-timeout", "5 seconds"),
      ("oxygenium.api.api-key-enabled", apiKeyEnabled),
      ("oxygenium.api.api-key", apiKeyValue),
      ("oxygenium.api.gas-fee-cap", "1000000000000000000"),
      ("oxygenium.api.default-utxos-limit", 512),
      ("oxygenium.api.max-form-buffered-bytes", 128 * 1024)
    )

    lazy val config = ConfigFactory
      .parseMap(
        configValues.view
          .mapValues {
            case value: AVector[_] =>
              ConfigValueFactory.fromIterable(value.toIterable.asJava)
            case value =>
              ConfigValueFactory.fromAnyRef(value)
          }
          .toMap
          .asJava
      )
    lazy val apiConfig = ApiConfig.load(config)
  }
}
