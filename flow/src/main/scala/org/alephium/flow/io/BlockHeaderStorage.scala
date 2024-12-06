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

import org.oxygenium.io._
import org.oxygenium.io.RocksDBSource.ColumnFamily
import org.oxygenium.protocol.model.{BlockHash, BlockHeader}

trait BlockHeaderStorage extends KeyValueStorage[BlockHash, BlockHeader] {
  def put(blockHeader: BlockHeader): IOResult[Unit] = put(blockHeader.hash, blockHeader)

  def putUnsafe(blockHeader: BlockHeader): Unit = putUnsafe(blockHeader.hash, blockHeader)

  def exists(blockHeader: BlockHeader): IOResult[Boolean] = exists(blockHeader.hash)

  def existsUnsafe(blockHeader: BlockHeader): Boolean = existsUnsafe(blockHeader.hash)

  def delete(blockHeader: BlockHeader): IOResult[Unit] = remove(blockHeader.hash)

  def deleteUnsafe(blockHeader: BlockHeader): Unit = removeUnsafe(blockHeader.hash)
}

object BlockHeaderRockDBStorage extends RocksDBKeyValueCompanion[BlockHeaderRockDBStorage] {
  def apply(
      storage: RocksDBSource,
      cf: ColumnFamily,
      writeOptions: WriteOptions,
      readOptions: ReadOptions
  ): BlockHeaderRockDBStorage = {
    new BlockHeaderRockDBStorage(storage, cf, writeOptions, readOptions)
  }
}

class BlockHeaderRockDBStorage(
    val storage: RocksDBSource,
    cf: ColumnFamily,
    writeOptions: WriteOptions,
    readOptions: ReadOptions
) extends RocksDBKeyValueStorage[BlockHash, BlockHeader](storage, cf, writeOptions, readOptions)
    with BlockHeaderStorage {}
