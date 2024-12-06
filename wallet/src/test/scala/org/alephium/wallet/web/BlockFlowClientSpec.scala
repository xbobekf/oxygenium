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

package org.oxygenium.wallet.web

import scala.concurrent.ExecutionContext

import org.scalatest.Inside
import sttp.client3._

import org.oxygenium.api.Endpoints
import org.oxygenium.api.model.{Amount, ApiKey, BuildTransferTx, Destination}
import org.oxygenium.http.EndpointSender
import org.oxygenium.json.Json._
import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.protocol.model._
import org.oxygenium.util.{OxygeniumSpec, AVector, Duration, U256}

class BlockFlowClientSpec() extends OxygeniumSpec with Inside {
  it should "correclty create an sttp request" in new Fixture {
    val destinations = AVector(Destination(toAddress, value, None, None))
    val buildTransferTransactionIn =
      BuildTransferTx(publicKey.bytes, None, destinations, None, None)
    val request =
      endpointSender.createRequest(
        buildTransferTransaction,
        buildTransferTransactionIn,
        uri"http://127.0.0.1:1234"
      )
    request.uri is uri"http://127.0.0.1:1234/transactions/build"

    inside(request.body) { case body: StringBody =>
      read[BuildTransferTx](body.s) is buildTransferTransactionIn
    }
  }

  trait Fixture extends Endpoints with LockupScriptGenerators {
    implicit val executionContext: ExecutionContext =
      scala.concurrent.ExecutionContext.Implicits.global
    implicit val groupConfig: GroupConfig = new GroupConfig { val groups = 4 }
    val groupIndex                        = GroupIndex.unsafe(0)
    val (script, publicKey, _)            = addressGen(groupIndex).sample.get
    val toAddress                         = Address.Asset(script)
    val value                             = Amount(U256.unsafe(1000))
    val blockflowFetchMaxAge              = Duration.unsafe(1000)
    val apiKeys: AVector[ApiKey]          = AVector.empty[ApiKey]
    val endpointSender                    = new EndpointSender(apiKeys.headOption)
  }
}
