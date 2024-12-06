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

package org.oxygenium.protocol.vm.event

import org.oxygenium.crypto.Byte32
import org.oxygenium.io.{StagingKVStorage, ValueExists}
import org.oxygenium.protocol.model.ContractId
import org.oxygenium.protocol.vm.{LogStateRef, LogStates, LogStatesId}
import org.oxygenium.protocol.vm.nodeindexes.StagingPageCounter
import org.oxygenium.util.AVector

final class StagingLog(
    val eventLog: StagingKVStorage[LogStatesId, LogStates],
    val eventLogByHash: StagingKVStorage[Byte32, AVector[LogStateRef]],
    val eventLogPageCounter: StagingPageCounter[ContractId]
) extends MutableLog {

  def rollback(): Unit = {
    eventLog.rollback()
    eventLogByHash.rollback()
    eventLogPageCounter.rollback()
  }

  def commit(): Unit = {
    eventLog.commit()
    eventLogByHash.commit()
    eventLogPageCounter.commit()
  }

  def getNewLogs(): AVector[LogStates] = {
    eventLog.caches.foldLeft(AVector.empty[LogStates]) {
      case (acc, (_, updated: ValueExists[LogStates] @unchecked)) => acc :+ updated.value
      case (acc, _)                                               => acc
    }
  }
}
