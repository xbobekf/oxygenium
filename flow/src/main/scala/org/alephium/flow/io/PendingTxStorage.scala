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

package org.oxygenium.flow.io

import org.rocksdb.{ReadOptions, WriteOptions}

import org.oxygenium.flow.model.PersistedTxId
import org.oxygenium.io._
import org.oxygenium.io.RocksDBSource.ColumnFamily
import org.oxygenium.protocol.model.TransactionTemplate

trait PendingTxStorage extends KeyValueStorage[PersistedTxId, TransactionTemplate] {
  def iterateE(f: (PersistedTxId, TransactionTemplate) => IOResult[Unit]): IOResult[Unit]
  def iterate(f: (PersistedTxId, TransactionTemplate) => Unit): IOResult[Unit]
  def replace(oldId: PersistedTxId, newId: PersistedTxId, tx: TransactionTemplate): IOResult[Unit]

  def size(): Int = {
    var result = 0
    iterate((_, _) => result += 1)
    result
  }
}

object PendingTxRocksDBStorage extends RocksDBKeyValueCompanion[PendingTxRocksDBStorage] {
  override def apply(
      storage: RocksDBSource,
      cf: ColumnFamily,
      writeOptions: WriteOptions,
      readOptions: ReadOptions
  ): PendingTxRocksDBStorage =
    new PendingTxRocksDBStorage(storage, cf, writeOptions, readOptions)
}

class PendingTxRocksDBStorage(
    val storage: RocksDBSource,
    cf: ColumnFamily,
    writeOptions: WriteOptions,
    readOptions: ReadOptions
) extends RocksDBKeyValueStorage[PersistedTxId, TransactionTemplate](
      storage,
      cf,
      writeOptions,
      readOptions
    )
    with PendingTxStorage {

  override def replace(
      oldId: PersistedTxId,
      newId: PersistedTxId,
      tx: TransactionTemplate
  ): IOResult[Unit] = {
    assume(oldId.txId == newId.txId)
    IOUtils.tryExecute {
      removeUnsafe(oldId)
      putUnsafe(newId, tx)
    }
  }
}
