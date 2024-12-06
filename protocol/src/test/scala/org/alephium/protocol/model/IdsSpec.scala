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

package org.oxygenium.protocol.model

import org.oxygenium.crypto.Blake3
import org.oxygenium.protocol.Hash
import org.oxygenium.util.OxygeniumSpec

class IdsSpec extends OxygeniumSpec {
  it should "check equality for general hashes" in {
    val hash0 = Hash.hash("hello")
    val hash1 = Hash.hash("hello")
    val hash2 = Hash.hash("world")

    def test[T](f: Hash => T) = {
      f(hash0) is f(hash1)
      f(hash0) isnot f(hash2)
    }

    test(identity)
    test(TransactionId.unsafe)
    test(ContractId.unsafe)
    test(TokenId.unsafe)
  }

  it should "check equality for blake hash" in {
    val hash0 = Blake3.hash("hello")
    val hash1 = Blake3.hash("hello")
    val hash2 = Blake3.hash("world")

    BlockHash.unsafe(hash0) is BlockHash.unsafe(hash1)
    BlockHash.unsafe(hash0) isnot BlockHash.unsafe(hash2)
  }
}
