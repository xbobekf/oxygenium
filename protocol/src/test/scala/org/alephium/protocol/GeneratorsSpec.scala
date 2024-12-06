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

package org.oxygenium.protocol

import org.scalacheck.Gen

import org.oxygenium.protocol.config.GroupConfigFixture
import org.oxygenium.util.OxygeniumSpec

class GeneratorsSpec extends OxygeniumSpec with Generators with GroupConfigFixture {
  override val groups = 3

  it should "generate random hashes" in {
    val sampleSize = 1000
    val hashes     = Gen.listOfN(sampleSize, hashGen).sample.get
    hashes.toSet.size is sampleSize
  }
}
