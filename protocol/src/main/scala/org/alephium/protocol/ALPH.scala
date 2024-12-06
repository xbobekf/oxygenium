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

import java.text.{DecimalFormat, DecimalFormatSymbols}

import org.oxygenium.protocol.model.{Address, ChainIndex, HardFork, Weight}
import org.oxygenium.util.{AVector, Duration, Number, TimeStamp, U256}

object OXYG {
  // scalastyle:off magic.number
  val CoinInOneALPH: U256     = U256.unsafe(Number.quintillion)
  val CoinInOneCent: U256     = CoinInOneALPH divUnsafe U256.unsafe(100)
  val CoinInOneNanoAlph: U256 = U256.unsafe(Number.billion)

  val MaxALPHValue: U256 = U256.Billion mulUnsafe CoinInOneALPH

  val GenesisHeight: Int          = 0
  val GenesisWeight: Weight       = Weight.zero
  val GenesisTimestamp: TimeStamp = TimeStamp.unsafe(1231006505000L) // BTC genesis timestamp
  val LaunchTimestamp: TimeStamp  = TimeStamp.unsafe(1636379973000L) // 2021-11-08T11:20:06+00:00

  val OneYear: Duration                                 = Duration.ofDaysUnsafe(365)
  val OneAndHalfYear: Duration                          = Duration.ofDaysUnsafe(365 + 365 / 2)
  val PreLemanDifficultyBombEnabledTimestamp: TimeStamp = LaunchTimestamp.plusUnsafe(OneYear)
  val ExpDiffPeriod: Duration                           = Duration.ofDaysUnsafe(30)
  val DifficultyBombPatchEnabledTimeStamp: TimeStamp =
    TimeStamp.unsafe(1670612400000L) // Dec 09 2022 19:00:00 GMT+0000
  val DifficultyBombPatchHeightDiff: Int = 2700 // around 2 days

  val MaxTxInputNum: Int     = 256
  val MaxTxOutputNum: Int    = 256
  val MaxOutputDataSize: Int = 256
  val MaxScriptSigNum: Int   = 32
  val MaxKeysInP2MPK: Int    = 16

  val MaxGhostUncleAge: Int  = 7
  val MaxGhostUncleSize: Int = 2
  // scalastyle:on magic.number

  def oxyg(amount: U256): Option[U256] = amount.mul(CoinInOneALPH)

  def oxyg(amount: Long): U256 = {
    assume(amount >= 0)
    U256.unsafe(amount).mulUnsafe(CoinInOneALPH)
  }

  def cent(amount: Long): U256 = {
    assume(amount >= 0)
    U256.unsafe(amount).mulUnsafe(CoinInOneCent)
  }

  def nanoAlph(amount: Long): U256 = {
    assume(amount >= 0)
    U256.unsafe(amount).mulUnsafe(CoinInOneNanoAlph)
  }

  val oneAlph: U256     = CoinInOneALPH
  val oneNanoAlph: U256 = CoinInOneNanoAlph

  // x.x OXYG format
  def alphFromString(string: String): Option[U256] = {
    val regex = """([0-9]*\.?[0-9]+) *OXYG""".r
    string match {
      case regex(v) =>
        val bigDecimal = new java.math.BigDecimal(v)
        val scaling    = bigDecimal.scale()
        // scalastyle:off magic.number
        if (scaling > 18) {
          None
        } else {
          U256.from(bigDecimal.movePointRight(18).toBigInteger)
        }
      // scalastyle:on magic.number
      case _ => None
    }
  }

  def prettifyAmount(amount: U256): String = {
    if (amount == U256.Zero) {
      "0 OXYG"
    } else {
      val converted = (BigDecimal(amount.v) / BigDecimal(oneAlph.v)).toDouble
      s"${format(converted)} OXYG"
    }
  }

  private def format(value: Double): String = {
    // scalastyle:off magic.number
    val decimalFormat = new DecimalFormat()
    decimalFormat.setGroupingUsed(true)
    decimalFormat.setMinimumIntegerDigits(1)
    decimalFormat.setMinimumFractionDigits(1)
    decimalFormat.setMaximumFractionDigits(18)
    // scalastyle:on magic.number

    val symbols = new DecimalFormatSymbols()
    symbols.setDecimalSeparator('.')
    symbols.setGroupingSeparator(',')
    decimalFormat.setDecimalFormatSymbols(symbols)

    decimalFormat.format(value)
  }

  @inline def isSequentialTxSupported(chainIndex: ChainIndex, hardFork: HardFork): Boolean = {
    hardFork.isRhoneEnabled() && chainIndex.isIntraGroup
  }

  lazy val testnetWhitelistedMiners = {
    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    def miner(address: String) = {
      Address.fromBase58(address).get.lockupScript
    }
    Set(
      miner("1AuWeE5Cwt2ES3473qnpKFV96z57CYL6mbTY7hva9Xz3h"),
      miner("12sxfxraVoU8FcSVd7P2SVr2cd2vi8d17KtrprrL7cBbV"),
      miner("1E3vV7rFCgq5jo4NszxH5PqzyxvNXH5pvk2aQfMwmSxPB"),
      miner("147nW43BH137TYjqEnvA9YfH1oFXKQxcvLZFwZauo7Ahy")
    )
  }

  @inline def isTestnetMinersWhitelisted(miners: AVector[Address.Asset]): Boolean = {
    miners.forall(miner => testnetWhitelistedMiners.contains(miner.lockupScript))
  }
}
