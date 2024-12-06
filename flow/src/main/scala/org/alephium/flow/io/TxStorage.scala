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

import org.oxygenium.flow.core.BlockChain
import org.oxygenium.flow.core.BlockChain.{TxIndex, TxIndexes}
import org.oxygenium.io._
import org.oxygenium.io.RocksDBSource.ColumnFamily
import org.oxygenium.protocol.model.TransactionId
import org.oxygenium.util.AVector

trait TxStorage extends KeyValueStorage[TransactionId, TxIndexes] {
  def add(txId: TransactionId, txIndex: TxIndex): IOResult[Unit] = {
    getOpt(txId).flatMap {
      case Some(txIndexes) => put(txId, TxIndexes(txIndexes.indexes :+ txIndex))
      case None            => put(txId, TxIndexes(AVector(txIndex)))
    }
  }

  def addUnsafe(txId: TransactionId, txIndex: TxIndex): Unit = {
    getOptUnsafe(txId) match {
      case Some(txIndexes) => putUnsafe(txId, TxIndexes(txIndexes.indexes :+ txIndex))
      case None            => putUnsafe(txId, TxIndexes(AVector(txIndex)))
    }
  }
}

object TxRocksDBStorage extends RocksDBKeyValueCompanion[TxRocksDBStorage] {
  override def apply(
      storage: RocksDBSource,
      cf: ColumnFamily,
      writeOptions: WriteOptions,
      readOptions: ReadOptions
  ): TxRocksDBStorage =
    new TxRocksDBStorage(storage, cf, writeOptions, readOptions)
}

class TxRocksDBStorage(
    val storage: RocksDBSource,
    cf: ColumnFamily,
    writeOptions: WriteOptions,
    readOptions: ReadOptions
) extends RocksDBKeyValueStorage[TransactionId, BlockChain.TxIndexes](
      storage,
      cf,
      writeOptions,
      readOptions
    )
    with TxStorage {
  override def remove(key: TransactionId): IOResult[Unit] = ???

  override def removeUnsafe(key: TransactionId): Unit = ???
}
