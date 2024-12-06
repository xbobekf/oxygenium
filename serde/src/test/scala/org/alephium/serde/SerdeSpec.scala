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

package org.alephium.serde

import java.math.BigInteger
import java.net.InetSocketAddress

import scala.collection.immutable.ArraySeq

import akka.util.ByteString
import org.scalacheck.Gen

import org.alephium.serde.Serde.ByteSerde
import org.alephium.util._

class SerdeSpec extends AlephiumSpec {

  def checkException[T](serde: FixedSizeSerde[T]): Unit = {
    it should "throw correct exceptions when error occurs" in {
      forAll { (inputs: Array[Byte]) =>
        lazy val exception = serde.deserialize(ByteString(inputs)).leftValue
        if (inputs.length < serde.serdeSize) {
          exception is SerdeError.WrongFormat(s"Too few bytes: expected 1, got 0")
        } else if (inputs.length > serde.serdeSize) {
          exception is SerdeError.WrongFormat(s"Too many bytes: expected 1, got ${inputs.length}")
        }
      }
    }
  }

  "Serde for Byte" should "serialize Byte into 1 byte" in {
    byteSerde.asInstanceOf[FixedSizeSerde[Byte]].serdeSize is 1
  }

  it should "serde correctly" in {
    forAll { (b: Byte) =>
      val bb = deserialize[Byte](serialize(b))
      bb isE b
    }

    forAll { (b: Byte) =>
      val input  = ByteString(b)
      val output = serialize(deserialize[Byte](input).toOption.get)
      output is input
    }

    serialize(0.toByte) is ByteString(0)
    serialize(127.toByte) is ByteString(127)
    serialize(-128.toByte) is ByteString(-128)
    deserialize[Byte](ByteString.empty).leftValue is a[SerdeError.WrongFormat]
  }

  checkException(ByteSerde)

  "Serde for Int" should "serde correctly" in {
    forAll { (n: Int) =>
      val nn = deserialize[Int](serialize(n))
      nn isE n
    }

    serialize[Int](0) is ByteString(0)
    serialize[Int](Int.MaxValue) is ByteString(0xc0, 127, -1, -1, -1)
    serialize[Int](Int.MinValue) is ByteString(0xc0, -128, 0, 0, 0)
    deserialize[Int](ByteString.empty).leftValue is a[SerdeError.WrongFormat]
  }

  "Serde for I256" should "serde correct" in {
    val cases = List(I256.Zero, I256.One, I256.NegOne, I256.MaxValue, I256.MinValue)
    for (n <- cases) {
      deserialize[I256](serialize(n)) isE n
    }

    serialize[I256](I256.Zero) is ByteString(0)
    serialize[I256](I256.MaxValue) is ByteString(0xc0 + 32 - 4, 127) ++
      ByteString.fromArrayUnsafe(Array.fill(31)(-1))
    serialize[I256](I256.MinValue) is ByteString(0xc0 + 32 - 4, -128) ++
      ByteString.fromArrayUnsafe(Array.fill(31)(0))
    deserialize[I256](ByteString.empty).leftValue is a[SerdeError.WrongFormat]
  }

  "Serde for U256" should "serde correct" in {
    val cases = List(U256.Zero, U256.One, U256.MaxValue, U256.MinValue)
    for (n <- cases) {
      deserialize[U256](serialize(n)) isE n
    }

    serialize[U256](U256.Zero) is ByteString(0)
    serialize[U256](U256.MaxValue) is ByteString(0xc0 + 32 - 4, -1) ++
      ByteString.fromArrayUnsafe(Array.fill(31)(-1))
    deserialize[U256](ByteString.empty).leftValue is a[SerdeError.WrongFormat]
  }

  "Serde for ByteString" should "serialize correctly" in {
    deserialize[ByteString](serialize(ByteString.empty)) isE ByteString.empty
    forAll { (n: Int) =>
      val bs = ByteString.fromInts(n)
      deserialize[ByteString](serialize(bs)) isE bs
    }
  }

  it should "fail for negative length" in {
    deserialize[ByteString](serialize[Int](-1)).leftValue is
      SerdeError.validation(s"Negative byte string length: -1")
  }

  "Serde for String" should "serialize correctly" in {
    forAll { (s: String) => deserialize[String](serialize(s)) isE s }
  }

  "Serde for BigInteger" should "serde correctly" in {
    forAll { (n: Long) =>
      val bn = BigInteger.valueOf(n)
      deserialize[BigInteger](serialize(bn)) isE bn
    }

    deserialize[BigInteger](ByteString.empty).leftValue is a[SerdeError.WrongFormat]
  }

  "Serde for fixed size sequence" should "serde correctly" in {
    forAll { (input: AVector[Byte]) =>
      {
        val serde  = Serde.fixedSizeSerde(input.length, byteSerde)
        val output = serde.deserialize(serde.serialize(input)).toOption.get
        output is input
      }
      {
        val serde     = Serde.fixedSizeSerde(input.length + 1, byteSerde)
        val exception = serde.deserialize(ByteString(input.toArray)).leftValue
        exception is a[SerdeError.Validation]
      }

      if (input.nonEmpty) {
        val serde     = Serde.fixedSizeSerde(input.length - 1, byteSerde)
        val exception = serde.deserialize(ByteString(input.toArray)).leftValue
        exception is a[SerdeError.WrongFormat]
      }
    }
  }

  "Serde for sequence" should "serde correctly" in {
    forAll { (input: AVector[Byte]) =>
      deserialize[AVector[Byte]](serialize(input)) isE input
      val seq = ArraySeq.from(input.toIterable)
      deserialize[ArraySeq[Byte]](serialize(seq)) isE seq
    }
  }

  it should "return error for invalid array size" in {
    deserialize[AVector[Byte]](serialize[Int](-100)).leftValue is
      SerdeError.validation(s"Negative array size: -100")
    deserialize[AVector[Byte]](serialize[Int](1)).leftValue is
      SerdeError.validation(s"Malicious array size: 1")
  }

  "Serde for option" should "work" in {
    forAll(Gen.option(Gen.choose(0, Int.MaxValue))) { input =>
      deserialize[Option[Int]](serialize(input)) isE input
    }
  }

  "Serde for either" should "work" in {
    forAll { (left: Int, right: Byte) =>
      val input1: Either[Int, Byte] = Left(left)
      deserialize[Either[Int, Byte]](serialize(input1)) isE input1
      val input2: Either[Int, Byte] = Right(right)
      deserialize[Either[Int, Byte]](serialize(input2)) isE input2
    }
  }

  case class Test1(x: Int)
  object Test1 {
    implicit val serde: Serde[Test1] = Serde.forProduct1(apply, t => t.x)
  }

  case class Test2(x: Int, y: Int)
  object Test2 {
    implicit val serde: Serde[Test2] = Serde.forProduct2(apply, t => (t.x, t.y))
  }

  case class Test3(x: Int, y: Int, z: Int)
  object Test3 {
    implicit val serde: Serde[Test3] = Serde.forProduct3(apply, t => (t.x, t.y, t.z))
  }

  "Serde for case class" should "serde one field correctly" in {
    forAll { (x: Int) =>
      val input  = Test1(x)
      val output = deserialize[Test1](serialize(input))
      output isE input
    }
  }

  it should "serde two fields correctly" in {
    forAll { (x: Int, y: Int) =>
      val input  = Test2(x, y)
      val output = deserialize[Test2](serialize(input))
      output isE input
    }
  }

  it should "serde three fields correctly" in {
    forAll { (x: Int, y: Int, z: Int) =>
      val input  = Test3(x, y, z)
      val output = deserialize[Test3](serialize(input))
      output isE input
    }
  }

  "Serde for address" should "serde correctly" in {
    val address0 = new InetSocketAddress("127.0.0.1", 9000)
    val output0  = deserialize[InetSocketAddress](serialize(address0))
    output0 isE address0

    val address1 = new InetSocketAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334", 9000)
    val output1  = deserialize[InetSocketAddress](serialize(address1))
    output1 isE address1
  }

  // scalastyle:off regex
  it should "fail for address based on host name" in {
    val address1 = serialize("localhost") ++ serialize("9000")
    deserialize[InetSocketAddress](address1).leftValue is a[SerdeError.WrongFormat]

    val address2 = serialize("127.0.0.1") ++ serialize("9000")
    deserialize[InetSocketAddress](address2).leftValue is a[SerdeError.WrongFormat]
  }
  // scalastyle:on regex

  it should "serde timestamp" in {
    forAll { (millis: Long) =>
      if (millis >= 0) {
        val ts = TimeStamp.from(millis).get
        deserialize[TimeStamp](serialize(ts)) isE ts
      } else {
        deserialize[TimeStamp](Bytes.from(millis)).leftValue is a[SerdeError.Validation]
      }
    }

    serialize(TimeStamp.zero) is ByteString(0, 0, 0, 0, 0, 0, 0, 0)
    serialize(TimeStamp.unsafe(Long.MaxValue)) is ByteString(127, -1, -1, -1, -1, -1, -1, -1)

    deserialize[TimeStamp](ByteString.empty).leftValue is a[SerdeError.WrongFormat]
  }
}
