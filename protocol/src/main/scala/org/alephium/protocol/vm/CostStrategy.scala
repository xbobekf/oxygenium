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

package org.alephium.protocol.vm

import akka.util.ByteString

import org.alephium.protocol.model.HardFork
import org.alephium.util.U256

trait CostStrategy {
  var gasRemaining: GasBox
  var gasFeePaid: U256 = U256.Zero

  def chargeGas(instr: GasSimple): ExeResult[Unit] = chargeGas(instr.gas())

  def chargeGasWithSize(instr: GasFormula, size: Int): ExeResult[Unit] = {
    chargeGas(instr.gas(size))
  }

  def chargeContractLoad(obj: StatefulContractObject): ExeResult[Unit] = {
    chargeContractLoad(obj.estimateContractLoadByteSize())
  }

  def chargeContractLoad(size: Int): ExeResult[Unit] = {
    chargeGas(GasSchedule.contractLoadGas(size))
  }

  def chargeContractStateUpdate(obj: StatefulContractObject): ExeResult[Unit] = {
    chargeContractStateUpdate(obj.mutFields)
  }

  def chargeContractStateUpdate(fields: Iterable[Val]): ExeResult[Unit] = {
    for {
      _ <- chargeGas(GasSchedule.contractStateUpdateBaseGas)
      _ <- chargeFieldSize(fields)
    } yield ()
  }

  def chargeContractInput(): ExeResult[Unit] = chargeGas(GasSchedule.txInputBaseGas)

  def chargeGeneratedOutput(): ExeResult[Unit] = chargeGas(GasSchedule.txOutputBaseGas)

  def chargeGas(gas: GasBox): ExeResult[Unit] = {
    updateGas(gasRemaining.use(gas))
  }

  def chargeFieldSize(fields: Iterable[Val]): ExeResult[Unit] = {
    updateGas(VM.checkFieldSize(gasRemaining, fields))
  }

  def chargeContractCodeSize(codeBytes: ByteString, hardFork: HardFork): ExeResult[Unit] = {
    updateGas(VM.checkCodeSize(gasRemaining, codeBytes, hardFork))
  }

  def chargeHash(byteLength: Int): ExeResult[Unit] = {
    chargeGas(GasHash.gas(byteLength))
  }

  def chargeDoubleHash(byteLength: Int): ExeResult[Unit] = {
    for {
      _ <- chargeHash(byteLength)
      _ <- chargeHash(32)
    } yield ()
  }

  @inline private def updateGas(f: => ExeResult[GasBox]): ExeResult[Unit] = {
    f.map(gasRemaining = _)
  }

  def payGasFee(amount: U256): ExeResult[Unit] = {
    gasFeePaid.add(amount) match {
      case Some(paid) =>
        gasFeePaid = paid
        okay
      case None =>
        failed(GasOverflow)
    }
  }
}
