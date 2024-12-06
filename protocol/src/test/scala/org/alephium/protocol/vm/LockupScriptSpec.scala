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

package org.oxygenium.protocol.vm

import org.oxygenium.protocol.Hash
import org.oxygenium.protocol.model.{ContractId, NoIndexModelGenerators}
import org.oxygenium.serde._
import org.oxygenium.util.{OxygeniumSpec, AVector, Hex}

class LockupScriptSpec extends OxygeniumSpec with NoIndexModelGenerators {
  it should "serde correctly" in {
    forAll(groupIndexGen.flatMap(assetLockupGen)) { lock =>
      serialize[LockupScript](lock) is serialize[LockupScript.Asset](lock)
      deserialize[LockupScript](serialize[LockupScript](lock)) isE lock
      deserialize[LockupScript.Asset](serialize[LockupScript.Asset](lock)) isE lock
    }

    forAll(groupIndexGen.flatMap(p2cLockupGen)) { lock =>
      deserialize[LockupScript](serialize[LockupScript](lock)) isE lock
      deserialize[LockupScript.Asset](serialize[LockupScript](lock)).leftValue is
        a[SerdeError.Validation]
    }
  }

  it should "serialize the examples" in {
    val hash0 = Hash.random
    val hash1 = Hash.random

    val lock0 = LockupScript.p2pkh(hash0)
    serialize[LockupScript](lock0) is Hex.unsafe(s"00${hash0.toHexString}")

    val lock1 = LockupScript.P2MPKH.unsafe(AVector(hash0, hash1), 1)
    serialize[LockupScript](lock1) is Hex.unsafe(s"0102${hash0.toHexString}${hash1.toHexString}01")

    val lock2 = LockupScript.p2sh(hash0)
    serialize[LockupScript](lock2) is Hex.unsafe(s"02${hash0.toHexString}")

    val lock3 = LockupScript.p2c(ContractId.unsafe(hash0))
    serialize[LockupScript](lock3) is Hex.unsafe(s"03${hash0.toHexString}")
  }

  it should "validate multisig" in {
    val hash0 = Hash.random
    val hash1 = Hash.random

    val lock0 = Hex.unsafe(s"0102${hash0.toHexString}${hash1.toHexString}00")
    deserialize[LockupScript](lock0).leftValue.getMessage
      .startsWith(s"Invalid m in m-of-n multisig") is true

    val lock1 = Hex.unsafe(s"0102${hash0.toHexString}${hash1.toHexString}03")
    deserialize[LockupScript](lock1).leftValue.getMessage
      .startsWith(s"Invalid m in m-of-n multisig") is true

    val lock2 = Hex.unsafe(s"0102${hash0.toHexString}${hash1.toHexString}01")
    deserialize[LockupScript](lock2).isRight is true

    val lock3 = Hex.unsafe(s"0102${hash0.toHexString}${hash1.toHexString}02")
    deserialize[LockupScript](lock3).isRight is true

    val lock4 = Hex.unsafe(s"010000")
    deserialize[LockupScript](lock4).leftValue.getMessage
      .startsWith(s"Invalid m in m-of-n multisig") is true
  }
}
