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

package org.oxygenium.io

import org.oxygenium.macros.EnumerationMacros
import org.oxygenium.util.{OxygeniumSpec, AVector}

class RocksDBStorageSpec extends OxygeniumSpec {
  import RocksDBSource.ColumnFamily

  behavior of "RocksDBStorage"

  implicit val ordering: Ordering[ColumnFamily] = Ordering.by(_.name)

  it should "index all column family" in {
    val xs = EnumerationMacros.sealedInstancesOf[ColumnFamily]
    ColumnFamily.values is AVector.from(xs)
  }
}
