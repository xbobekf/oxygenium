// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
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

package org.alephium.protocol.model

import org.alephium.util.AlephiumSpec

class GroupIndexSpec extends AlephiumSpec with NoIndexModelGenerators {
  it should "equalize same values" in {
    forAll(groupIndexGen) { n =>
      val groupIndex1 = GroupIndex.unsafe(n.value)(groupConfig)
      val groupIndex2 = GroupIndex.unsafe(n.value)(groupConfig)
      groupIndex1 is groupIndex2
    }

    val index1 = new GroupIndex(999999)
    val index2 = new GroupIndex(999999)
    index1 is index2
  }
}
