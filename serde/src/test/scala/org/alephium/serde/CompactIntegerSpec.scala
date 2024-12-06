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

package org.oxygenium.serde

import akka.util.ByteString
import org.scalatest.Assertion

import org.oxygenium.util._

class CompactIntegerSpec extends OxygeniumSpec {
  it should "encode/decode U32 & U256" in {
    import CompactInteger.Unsigned._

    def test(n: Int): Assertion = {
      val u32 = U32.unsafe(n)
      decodeU32(encode(u32)) isE Staging(u32, ByteString.empty)

      val u256 = U256.unsafe(Integer.toUnsignedLong(n))
      decodeU256(encode(u256)) isE Staging(u256, ByteString.empty)
    }

    (0 until 32).foreach { k =>
      test(1 << k)
      test((1 << k) - 1)
      test((1 << k) + 1)
    }

    decodeU32(encode(U32.MinValue)) isE Staging(U32.MinValue, ByteString.empty)
    decodeU32(encode(U32.MaxValue)) isE Staging(U32.MaxValue, ByteString.empty)
    decodeU256(encode(U256.MinValue)) isE Staging(U256.MinValue, ByteString.empty)
    decodeU256(encode(U256.MaxValue)) isE Staging(U256.MaxValue, ByteString.empty)

    forAll { (n: Int) => test(n) }

    forAll { (_: Int) =>
      val u256 = UnsecureRandom.nextU256()
      decodeU256(encode(u256)) isE Staging(u256, ByteString.empty)
    }
  }

  it should "encode/decode int" in {
    import CompactInteger.Signed._

    def test(n: Int): Assertion = {
      decodeInt(encode(n)) isE Staging(n, ByteString.empty)
      decodeLong(encode(n.toLong)) isE Staging(n.toLong, ByteString.empty)

      val i256 = I256.from(n)
      decodeI256(encode(i256)) isE Staging(i256, ByteString.empty)
    }

    (0 until 32).foreach { k =>
      test(1 << k)
      test((1 << k) - 1)
      test((1 << k) + 1)
      test(-(1 << k))
      test(-(1 << k) - 1)
      test(-(1 << k) + 1)
    }

    decodeInt(encode(Int.MaxValue)) isE Staging(Int.MaxValue, ByteString.empty)
    decodeInt(encode(Int.MinValue)) isE Staging(Int.MinValue, ByteString.empty)
    decodeLong(encode(Long.MaxValue)) isE Staging(Long.MaxValue, ByteString.empty)
    decodeLong(encode(Long.MinValue)) isE Staging(Long.MinValue, ByteString.empty)
    decodeI256(encode(I256.MaxValue)) isE Staging(I256.MaxValue, ByteString.empty)
    decodeI256(encode(I256.MinValue)) isE Staging(I256.MinValue, ByteString.empty)

    forAll { (n: Int) => test(n) }

    forAll { (_: Int) =>
      val i256 = UnsecureRandom.nextI256()
      decodeI256(encode(i256)) isE Staging(i256, ByteString.empty)
    }
  }

  it should "serialize examples" in {
    import CompactInteger._
    Signed.encode(0) is Hex.unsafe("00")
    Signed.encode(1) is Hex.unsafe("01")
    Signed.encode(-1) is Hex.unsafe("3F")
  }
}
