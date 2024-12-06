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

package org.alephium.protocol.vm

import akka.util.ByteString

import org.alephium.protocol.model.{minimalGas, HardFork}
import org.alephium.util.AlephiumSpec

class CostStrategySpec extends AlephiumSpec {
  it should "charge gas" in {
    def test(charge: CostStrategy => ExeResult[Unit], cost: Int) = {
      val strategy = new CostStrategy {
        override var gasRemaining: GasBox = minimalGas
      }
      charge(strategy)
      strategy.gasRemaining.addUnsafe(GasBox.unsafe(cost)) is minimalGas
    }

    val bytes = ByteString.fromArrayUnsafe(Array.ofDim[Byte](123))
    test(_.chargeGas(Pop), 2)
    test(_.chargeGasWithSize(Blake2b, 33), 60)
    test(_.chargeContractLoad(123), 800 + (123 + 7) / 8)
    test(_.chargeContractStateUpdate(Seq(Val.ByteVec(bytes))), 5000 + 123)
    test(_.chargeGas(GasBox.unsafe(100)), 100)
    test(_.chargeContractCodeSize(bytes, HardFork.Mainnet), 200 + 123)
    test(_.chargeContractCodeSize(bytes, HardFork.SinceLemanForTest), 200 + 123)
    test(_.chargeFieldSize(Seq(Val.ByteVec(bytes))), 123)
  }
}
