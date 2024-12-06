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

package org.oxygenium.protocol

import scala.util.Random

import org.oxygenium.protocol.config.GroupConfigFixture
import org.oxygenium.protocol.model.{Address, ChainIndex, HardFork}
import org.oxygenium.protocol.vm.LockupScript
import org.oxygenium.util.{OxygeniumSpec, AVector, NumericHelpers, U256}

class ALPHSpec extends OxygeniumSpec {
  import OXYG._

  it should "use correct unit" in {
    oxyg(1) is nanoAlph(1).mul(U256.Billion).get
    oxyg(1).toBigInt.longValue() is math.pow(10, 18).longValue()
    cent(1).mulUnsafe(U256.unsafe(100)) is oxyg(1)

    oneAlph is oxyg(1)
    oneNanoAlph is nanoAlph(1)
    oneAlph is (oneNanoAlph.mulUnsafe(U256.unsafe(1000000000)))
  }

  it should "parse `x.y OXYG` format" in new Fixture {
    check("1.2ALPH", oxyg(12) / 10)
    check("1.2 OXYG", oxyg(12) / 10)
    check("1 OXYG", oxyg(1))
    check("1ALPH", oxyg(1))
    check("0.1ALPH", oxyg(1) / 10)
    check(".1ALPH", oxyg(1) / 10)
    check(".1     OXYG", oxyg(1) / 10)
    check("0 OXYG", U256.Zero)
    check("1234.123456 OXYG", oxyg(1234123456) / 1000000)

    val alphMax = s"${MaxALPHValue.divUnsafe(oneAlph)}"
    alphMax is "1000000000"
    check(s"$alphMax OXYG", MaxALPHValue)

    fail("1.2alph")
    fail("-1.2alph")
    fail("1.2 oxyg")
    fail("1 Alph")
    fail("1. OXYG")
    fail(". OXYG")
    fail(" OXYG")
    fail("0.000000000000000000001 OXYG")
  }

  it should "pretty format" in new Fixture {
    def check(alphAmount: String, str: String) = {
      val amount = OXYG.alphFromString(s"$alphAmount OXYG").get
      OXYG.prettifyAmount(amount) is s"$str OXYG"
    }

    check("0", "0")
    check("1", "1.0")
    check("100", "100.0")
    check("1000", "1,000.0")
    check("1000000", "1,000,000.0")
    check("1000000.0", "1,000,000.0")
    check("1000000.011", "1,000,000.011")
    check("0.001", "0.001")
    check("0.000000001", "0.000000001")
    check("0.000000000000000001", "0.000000000000000001")
  }

  trait Fixture extends NumericHelpers {

    def check(str: String, expected: U256) = {
      OXYG.alphFromString(str) is Some(expected)
    }
    def fail(str: String) = {
      OXYG.alphFromString(str) is None
    }
  }

  it should "test isSequentialTxSupported" in new GroupConfigFixture.Default {
    OXYG.isSequentialTxSupported(ChainIndex.unsafe(0, 0), HardFork.Rhone) is true
    OXYG.isSequentialTxSupported(ChainIndex.unsafe(0, 1), HardFork.Rhone) is false
    OXYG.isSequentialTxSupported(ChainIndex.unsafe(0, 0), HardFork.PreRhoneForTest) is false
    OXYG.isSequentialTxSupported(ChainIndex.unsafe(0, 1), HardFork.PreRhoneForTest) is false
  }

  it should "test isTestnetMinersWhitelisted" in {
    val validMiners = AVector.from(
      OXYG.testnetWhitelistedMiners.map(lockupScript =>
        Address.from(lockupScript).asInstanceOf[Address.Asset]
      )
    )
    OXYG.isTestnetMinersWhitelisted(validMiners) is true
    validMiners.foreach(miner => OXYG.isTestnetMinersWhitelisted(AVector(miner)) is true)
    val index        = Random.nextInt(validMiners.length)
    val invalidMiner = Address.Asset(LockupScript.p2pkh(PublicKey.generate))
    OXYG.isTestnetMinersWhitelisted(validMiners.replace(index, invalidMiner)) is false
  }
}
