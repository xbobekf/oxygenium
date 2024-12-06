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

package org.alephium.protocol.vm

import org.alephium.protocol.model._
import org.alephium.util.AlephiumSpec

class GasBoxSpec extends AlephiumSpec {
  it should "validate gas bound" in {
    GasBox.validate(minimalGas, HardFork.Leman) is true
    GasBox.validate(GasBox.unsafe(minimalGas.value - 1), HardFork.Leman) is false
    GasBox.validate(maximalGasPerTxPreRhone, HardFork.Leman) is true
    GasBox.validate(GasBox.unsafe(maximalGasPerTxPreRhone.value + 1), HardFork.Leman) is false

    GasBox.validate(minimalGas, HardFork.Rhone) is true
    GasBox.validate(GasBox.unsafe(minimalGas.value - 1), HardFork.Rhone) is false
    GasBox.validate(maximalGasPerTxPreRhone, HardFork.Rhone) is true
    GasBox.validate(GasBox.unsafe(maximalGasPerTxPreRhone.value + 1), HardFork.Rhone) is true
    GasBox.validate(maximalGasPerTx, HardFork.Rhone) is true
    GasBox.validate(GasBox.unsafe(maximalGasPerTx.value + 1), HardFork.Rhone) is false
  }
}
