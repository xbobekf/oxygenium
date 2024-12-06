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

import akka.util.ByteString

import org.oxygenium.protocol.model.Transaction
import org.oxygenium.serde._
import org.oxygenium.util.AVector

final case class RichTransaction(
    unsigned: RichUnsignedTx,
    scriptExecutionOk: Boolean,
    contractInputs: AVector[RichContractInput],
    generatedOutputs: AVector[Output],
    inputSignatures: AVector[ByteString],
    scriptSignatures: AVector[ByteString]
)

object RichTransaction {
  def from(
      transaction: Transaction,
      assetInputs: AVector[RichAssetInput],
      contractInputs: AVector[RichContractInput]
  ): RichTransaction = {
    val richUnsigned = RichUnsignedTx.fromProtocol(transaction.unsigned, assetInputs)

    RichTransaction(
      unsigned = richUnsigned,
      scriptExecutionOk = transaction.scriptExecutionOk,
      contractInputs = contractInputs,
      transaction.generatedOutputs.zipWithIndex.map { case (out, index) =>
        Output.from(out, transaction.unsigned.id, index + transaction.unsigned.fixedOutputs.length)
      },
      inputSignatures = transaction.inputSignatures.map(sig => serialize(sig)),
      scriptSignatures = transaction.scriptSignatures.map(sig => serialize(sig))
    )
  }
}
