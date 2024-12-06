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

package org.oxygenium.protocol.model

import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.serde.Serde
import org.oxygenium.util.Bytes

// No substypes for the sake of performance
final case class Hint private (value: Int) extends AnyVal {
  def isAssetType: Boolean = (value & 1) == 1

  def isContractType: Boolean = (value & 1) == 0

  def decode: (ScriptHint, Boolean) = (scriptHint, isAssetType)

  def scriptHint: ScriptHint = new ScriptHint(value | 1)

  def groupIndex(implicit config: GroupConfig): GroupIndex = scriptHint.groupIndex
}

object Hint {
  // We don't use Serde[Int] here as the value of Hint is random, no need of serde optimization
  implicit val serde: Serde[Hint] = Serde
    .bytesSerde(4)
    .xmap(bs => new Hint(Bytes.toIntUnsafe(bs)), hint => Bytes.from(hint.value))

  def from(assetOutput: AssetOutput): Hint = ofAsset(assetOutput.lockupScript.scriptHint)

  def from(contractOutput: ContractOutput): Hint =
    ofContract(contractOutput.lockupScript.scriptHint)

  def ofAsset(scriptHint: ScriptHint): Hint = new Hint(scriptHint.value)

  def ofContract(scriptHint: ScriptHint): Hint = new Hint(scriptHint.value ^ 1)

  def unsafe(value: Int): Hint = new Hint(value)
}
