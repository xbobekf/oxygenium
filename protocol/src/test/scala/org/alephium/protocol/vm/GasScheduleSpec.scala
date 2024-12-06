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

import org.oxygenium.protocol.model.defaultGasPerInput
import org.oxygenium.util.OxygeniumSpec

class GasScheduleSpec extends OxygeniumSpec {
  it should "validate default gases" in {
    (defaultGasPerInput >= GasSchedule.p2pkUnlockGas) is true
  }

  it should "charge gas for hash" in {
    GasHash.gas(33) is GasBox.unsafe(60)
    GasSchedule.p2pkUnlockGas is GasBox.unsafe(60 + 2000)
  }
}
