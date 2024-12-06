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
import org.oxygenium.protocol.model.BlockHash
import org.oxygenium.serde.{avectorSerde, Serde}
import org.oxygenium.util.AVector

/*
 * There are 2 * groups - 1 dependent hashes for each block
 * The first G - 1 hashes are from groups different from this group
 * The rest G hashes are from all the chain related to this group
 */
final case class BlockDeps private (deps: AVector[BlockHash]) extends AnyVal {
  def length: Int = deps.length

  def getOutDep(to: GroupIndex): BlockHash = outDeps(to.value)

  def parentHash(chainIndex: ChainIndex): BlockHash = getOutDep(chainIndex.to)

  def uncleHash(toIndex: GroupIndex): BlockHash = getOutDep(toIndex)

  def outDeps: AVector[BlockHash] = deps.drop(deps.length / 2)

  def inDeps: AVector[BlockHash] = deps.take(deps.length / 2)

  @inline def intraDep(chainIndex: ChainIndex): BlockHash = getOutDep(chainIndex.from)

  @inline def unorderedIntraDeps(
      groupOfTheDeps: GroupIndex
  )(implicit groupConfig: GroupConfig): AVector[BlockHash] = {
    val intraUncleHash = uncleHash(groupOfTheDeps)
    assume(ChainIndex.from(intraUncleHash).isIntraGroup)
    inDeps :+ uncleHash(groupOfTheDeps)
  }
}

object BlockDeps {
  implicit val serde: Serde[BlockDeps] = Serde.forProduct1(unsafe, t => t.deps)

  def unsafe(deps: AVector[BlockHash]): BlockDeps = {
    new BlockDeps(deps)
  }

  def build(deps: AVector[BlockHash])(implicit config: GroupConfig): BlockDeps = {
    require(deps.length == config.depsNum)
    new BlockDeps(deps)
  }
}
