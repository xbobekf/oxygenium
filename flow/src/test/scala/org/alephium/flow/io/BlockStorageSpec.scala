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

import org.oxygenium.io._
import org.oxygenium.protocol.model.NoIndexModelGenerators
import org.oxygenium.util.OxygeniumSpec

class BlockStorageSpec
    extends OxygeniumSpec
    with NoIndexModelGenerators
    with StorageSpec[BlockRockDBStorage] {
  import RocksDBSource.ColumnFamily

  override val dbname: String = "block-storage-spec"
  override val builder: RocksDBSource => BlockRockDBStorage =
    source => BlockRockDBStorage(source, ColumnFamily.All)

  it should "save and read blocks" in {
    forAll(blockGen) { block =>
      storage.exists(block.hash) isE false
      storage.existsUnsafe(block.hash) is false
      storage.put(block) isE ()
      storage.existsUnsafe(block.hash) is true
      storage.exists(block.hash) isE true
      storage.getUnsafe(block.hash) is block
      storage.get(block.hash) isE block
    }
  }

  it should "fail to delete" in {
    forAll(blockGen) { block =>
      storage.put(block) isE ()
      assertThrows[NotImplementedError] {
        storage.remove(block.hash)
      }
      assertThrows[NotImplementedError] {
        storage.removeUnsafe(block.hash)
      }
    }
  }
}
