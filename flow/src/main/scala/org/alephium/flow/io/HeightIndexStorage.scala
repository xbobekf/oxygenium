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

import akka.util.ByteString
import org.rocksdb.{ReadOptions, WriteOptions}

import org.oxygenium.flow.io.HeightIndexStorage.hashesSerde
import org.oxygenium.io.{RocksDBKeyValueStorage, RocksDBSource}
import org.oxygenium.io.RocksDBSource.ColumnFamily
import org.oxygenium.protocol.model.{BlockHash, ChainIndex}
import org.oxygenium.serde._
import org.oxygenium.util.{AVector, Bytes}

object HeightIndexStorage {
  implicit val hashesSerde: Serde[AVector[BlockHash]] = avectorSerde[BlockHash]
}

class HeightIndexStorage(
    chainIndex: ChainIndex,
    storage: RocksDBSource,
    cf: ColumnFamily,
    writeOptions: WriteOptions,
    readOptions: ReadOptions
) extends RocksDBKeyValueStorage[Int, AVector[BlockHash]](storage, cf, writeOptions, readOptions) {
  private val postFix =
    ByteString(chainIndex.from.value.toByte, chainIndex.to.value.toByte, Storages.heightPostfix)

  override def storageKey(key: Int): ByteString = Bytes.from(key) ++ postFix
}
