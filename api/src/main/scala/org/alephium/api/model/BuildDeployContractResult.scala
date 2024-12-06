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
import org.oxygenium.protocol.model.{Address, ContractId, TransactionId, UnsignedTransaction}
import org.oxygenium.protocol.vm.{GasBox, GasPrice}
import org.oxygenium.serde.serialize
import org.oxygenium.util.Hex

final case class BuildDeployContractTxResult(
    fromGroup: Int,
    toGroup: Int,
    unsignedTx: String,
    gasAmount: GasBox,
    gasPrice: GasPrice,
    txId: TransactionId,
    contractAddress: Address.Contract
) extends GasInfo
    with ChainIndexInfo
    with TransactionInfo

object BuildDeployContractTxResult {
  def from(
      unsignedTx: UnsignedTransaction
  )(implicit groupConfig: GroupConfig): BuildDeployContractTxResult = {
    val contractId =
      ContractId.from(unsignedTx.id, unsignedTx.fixedOutputs.length, unsignedTx.fromGroup)
    BuildDeployContractTxResult(
      unsignedTx.fromGroup.value,
      unsignedTx.toGroup.value,
      Hex.toHexString(serialize(unsignedTx)),
      unsignedTx.gasAmount,
      unsignedTx.gasPrice,
      unsignedTx.id,
      Address.contract(contractId)
    )
  }
}
