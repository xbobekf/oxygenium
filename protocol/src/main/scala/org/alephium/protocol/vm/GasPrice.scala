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

import org.oxygenium.protocol.ALPH
import org.oxygenium.protocol.model.{coinbaseGasPrice, nonCoinbaseMinGasPrice, HardFork}
import org.oxygenium.serde.{u256Serde, Serde}
import org.oxygenium.util.U256

final case class GasPrice(value: U256) extends Ordered[GasPrice] {
  // this is safe as value <= ALPH.MaxALPHValue
  def *(gas: GasBox): U256 = {
    value.mulUnsafe(gas.toU256)
  }

  override def compare(that: GasPrice): Int = this.value.compare(that.value)
}

object GasPrice {
  implicit val serde: Serde[GasPrice] = Serde.forProduct1(GasPrice.apply, _.value)

  @inline def validate(gasPrice: GasPrice, isCoinbase: Boolean, hardFork: HardFork): Boolean = {
    val minGasPrice =
      if (isCoinbase || !hardFork.isLemanEnabled()) coinbaseGasPrice else nonCoinbaseMinGasPrice
    gasPrice >= minGasPrice && gasPrice.value < ALPH.MaxALPHValue
  }
}
