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

import org.oxygenium.flow.model.ReadyTxInfo
import org.oxygenium.io._
import org.oxygenium.io.RocksDBSource.ColumnFamily
import org.oxygenium.protocol.model.TransactionId

trait ReadyTxStorage extends KeyValueStorage[TransactionId, ReadyTxInfo] {
  def iterateE(f: (TransactionId, ReadyTxInfo) => IOResult[Unit]): IOResult[Unit]
  def iterate(f: (TransactionId, ReadyTxInfo) => Unit): IOResult[Unit]
  def clear(): IOResult[Unit]
}

object ReadyTxRocksDBStorage extends RocksDBKeyValueCompanion[ReadyTxRocksDBStorage] {
  def apply(
      storage: RocksDBSource,
      cf: ColumnFamily,
      writeOptions: WriteOptions,
      readOptions: ReadOptions
  ): ReadyTxRocksDBStorage =
    new ReadyTxRocksDBStorage(storage, cf, writeOptions, readOptions)
}

class ReadyTxRocksDBStorage(
    val storage: RocksDBSource,
    cf: ColumnFamily,
    writeOptions: WriteOptions,
    readOptions: ReadOptions
) extends RocksDBKeyValueStorage[TransactionId, ReadyTxInfo](
      storage,
      cf,
      writeOptions,
      readOptions
    )
    with ReadyTxStorage {
  override def clear(): IOResult[Unit] = {
    iterateE((key, _) => remove(key))
  }
}
