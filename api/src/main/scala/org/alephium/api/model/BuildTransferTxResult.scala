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
package org.oxygenium.api.model

import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.protocol.model.{TransactionId, UnsignedTransaction}
import org.oxygenium.protocol.vm.{GasBox, GasPrice}
import org.oxygenium.serde.serialize
import org.oxygenium.util.Hex

final case class BuildTransferTxResult(
    unsignedTx: String,
    gasAmount: GasBox,
    gasPrice: GasPrice,
    txId: TransactionId,
    fromGroup: Int,
    toGroup: Int
) extends GasInfo
    with ChainIndexInfo
    with TransactionInfo

object BuildTransferTxResult {
  def from(
      unsignedTx: UnsignedTransaction
  )(implicit groupConfig: GroupConfig): BuildTransferTxResult =
    BuildTransferTxResult(
      Hex.toHexString(serialize(unsignedTx)),
      unsignedTx.gasAmount,
      unsignedTx.gasPrice,
      unsignedTx.id,
      unsignedTx.fromGroup.value,
      unsignedTx.toGroup.value
    )
}
