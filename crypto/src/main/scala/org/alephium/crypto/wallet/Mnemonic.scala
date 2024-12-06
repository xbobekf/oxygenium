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

package org.alephium.crypto.wallet

import java.nio.charset.StandardCharsets
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import scala.io.Source

import akka.util.ByteString

import org.alephium.crypto.Sha256
import org.alephium.util.{AVector, Bits, SecureAndSlowRandom}

//scalastyle:off magic.number

final class Mnemonic private (val words: AVector[String]) extends AnyVal {

  def toSeed(passphraseOpt: Option[String]): ByteString = {
    val mnemonic     = toLongString.toCharArray
    val extendedPass = s"mnemonic${passphraseOpt.getOrElse("")}".getBytes(StandardCharsets.UTF_8)
    val spec = new PBEKeySpec(
      mnemonic,
      extendedPass,
      Mnemonic.pbkdf2Iterations,
      Mnemonic.pbkdf2KeyLength
    )
    val factory = SecretKeyFactory.getInstance(Mnemonic.pbkdf2Algorithm)
    ByteString.fromArrayUnsafe(factory.generateSecret(spec).getEncoded)
  }

  def toLongString: String = words.mkString(" ")
}

object Mnemonic {
  final class Size private (val value: Int) extends AnyVal
  object Size {
    val list: AVector[Size] =
      AVector(new Size(12), new Size(15), new Size(18), new Size(21), new Size(24))

    def apply(size: Int): Option[Size] =
      Option.when(validate(size))(new Size(size))

    def validate(size: Int): Boolean = list.contains(new Size(size))
  }
  val entropySizes: Seq[Int] = Seq(16, 20, 24, 28, 32)

  val pbkdf2Algorithm: String = "PBKDF2WithHmacSHA512"
  val pbkdf2Iterations: Int   = 2048
  val pbkdf2KeyLength: Int    = 512

  lazy val englishWordlist: AVector[String] = {
    val stream = Mnemonic.getClass.getResourceAsStream("/bip39_english_wordlist.txt")
    AVector.from(Source.fromInputStream(stream, "UTF-8").getLines().to(Iterable))
  }

  def generate(size: Int): Option[Mnemonic] =
    Size(size).map(generate)

  def generate(size: Size): Mnemonic = {
    val typeIndex   = Size.list.indexWhere(_ == size)
    val entropySize = entropySizes(typeIndex)
    val rawEntropy  = Array.ofDim[Byte](entropySize)
    SecureAndSlowRandom.source.nextBytes(rawEntropy)
    val entropy = ByteString.fromArrayUnsafe(rawEntropy)
    fromEntropyUnsafe(entropy)
  }

  private def validateWords(words: Array[String]): Boolean = {
    Size.validate(words.length) && words.forall(englishWordlist.contains)
  }

  def from(input: String): Option[Mnemonic] = {
    val words = input.split(" ")
    Option.when(validateWords(words))(new Mnemonic(AVector.unsafe(words)))
  }

  def unsafe(words: AVector[String]): Mnemonic = {
    new Mnemonic(words)
  }

  protected[wallet] def validateEntropy(entropy: ByteString): Boolean = {
    entropySizes.contains(entropy.length)
  }

  protected[wallet] def unsafe(entropy: ByteString): Mnemonic = {
    val checkSum = Sha256.hash(entropy).bytes.take(1)
    val extendedEntropy = AVector
      .from(entropy ++ checkSum)
      .flatMap(Bits.from)
      .take(entropy.length * 8 + entropy.length / 4)
    val worldIndexes = extendedEntropy.grouped(11).map(Bits.toInt)
    new Mnemonic(worldIndexes.map(englishWordlist.apply))
  }

  def fromEntropyUnsafe(entropy: ByteString): Mnemonic = {
    assume(validateEntropy(entropy))
    unsafe(entropy)
  }

  def from(entropy: ByteString): Option[Mnemonic] = {
    Option.when(validateEntropy(entropy))(unsafe(entropy))
  }
}
