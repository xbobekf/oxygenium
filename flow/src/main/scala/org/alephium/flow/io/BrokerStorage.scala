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

import scala.collection.mutable

import org.rocksdb.{ReadOptions, WriteOptions}

import org.oxygenium.flow.model.BrokerDiscoveryState
import org.oxygenium.io._
import org.oxygenium.io.RocksDBSource.ColumnFamily
import org.oxygenium.protocol.model.{BrokerInfo, PeerId}
import org.oxygenium.util.AVector

trait BrokerStorage extends KeyValueStorage[PeerId, BrokerDiscoveryState] {
  def addBroker(brokerInfo: BrokerInfo): IOResult[Unit]
  def activeBrokers(): IOResult[AVector[BrokerInfo]]
}

object BrokerRocksDBStorage extends RocksDBKeyValueCompanion[BrokerRocksDBStorage] {
  override def apply(
      storage: RocksDBSource,
      cf: ColumnFamily,
      writeOptions: WriteOptions,
      readOptions: ReadOptions
  ): BrokerRocksDBStorage = {
    new BrokerRocksDBStorage(storage, cf, writeOptions, readOptions)
  }
}

class BrokerRocksDBStorage(
    val storage: RocksDBSource,
    cf: ColumnFamily,
    writeOptions: WriteOptions,
    readOptions: ReadOptions
) extends RocksDBKeyValueStorage[PeerId, BrokerDiscoveryState](
      storage,
      cf,
      writeOptions,
      readOptions
    )
    with BrokerStorage {
  override def addBroker(brokerInfo: BrokerInfo): IOResult[Unit] = {
    val state = BrokerDiscoveryState(brokerInfo.address, brokerInfo.brokerNum)
    put(brokerInfo.peerId, state)
  }

  override def activeBrokers(): IOResult[AVector[BrokerInfo]] = {
    val buffer = mutable.ArrayBuffer.empty[BrokerInfo]
    iterate((peerId, state) => {
      val brokerInfo = BrokerInfo.unsafe(
        peerId.cliqueId,
        peerId.brokerId,
        state.brokerNum,
        state.address
      )
      buffer += brokerInfo
    }).map(_ => AVector.from(buffer))
  }
}
