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

package org.oxygenium.protocol.vm

import org.oxygenium.crypto.Byte32
import org.oxygenium.io.{RocksDBSource, SparseMerkleTrie, StorageFixture}
import org.oxygenium.protocol.Hash
import org.oxygenium.protocol.model.{ContractId, TransactionId, TxOutputRef}
import org.oxygenium.protocol.vm.event.LogStorage
import org.oxygenium.protocol.vm.nodeindexes.NodeIndexesStorage
import org.oxygenium.protocol.vm.subcontractindex._
import org.oxygenium.serde.{avectorSerde, eitherSerde, intSerde}
import org.oxygenium.util.AVector

trait VMFactory extends StorageFixture {
  lazy val cachedWorldState: WorldState.Cached = {
    val storage = newDBStorage()
    val trieDb  = newDB[Hash, SparseMerkleTrie.Node](storage, RocksDBSource.ColumnFamily.Trie)
    val trieImmutableStateStorage =
      newDB[Hash, ContractStorageImmutableState](storage, RocksDBSource.ColumnFamily.Trie)
    val logDb        = newDB[LogStatesId, LogStates](storage, RocksDBSource.ColumnFamily.Log)
    val logRefDb     = newDB[Byte32, AVector[LogStateRef]](storage, RocksDBSource.ColumnFamily.Log)
    val logCounterDb = newDB[ContractId, Int](storage, RocksDBSource.ColumnFamily.LogCounter)
    val logStorage   = LogStorage(logDb, logRefDb, logCounterDb)
    val txOutputRefIndexStorage =
      newDB[TxOutputRef.Key, TransactionId](storage, RocksDBSource.ColumnFamily.TxOutputRefIndex)
    val parentContractIndexStorage =
      newDB[ContractId, ContractId](storage, RocksDBSource.ColumnFamily.ParentContract)
    val subContractIndexStateStorage = newDB[SubContractIndexStateId, SubContractIndexState](
      storage,
      RocksDBSource.ColumnFamily.SubContract
    )
    val subContractIndexCounterStorage =
      newDB[ContractId, Int](storage, RocksDBSource.ColumnFamily.SubContractCounter)
    val subContractIndexStorage = SubContractIndexStorage(
      parentContractIndexStorage,
      subContractIndexStateStorage,
      subContractIndexCounterStorage
    )
    val nodeIndexesStorage = NodeIndexesStorage(
      logStorage,
      Some(txOutputRefIndexStorage),
      Some(subContractIndexStorage)
    )

    WorldState.emptyCached(trieDb, trieImmutableStateStorage, nodeIndexesStorage)
  }
}
