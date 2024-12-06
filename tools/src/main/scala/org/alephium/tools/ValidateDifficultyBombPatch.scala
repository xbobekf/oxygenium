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

package org.oxygenium.tools

import java.nio.file.Path

import org.oxygenium.flow.core.BlockFlow
import org.oxygenium.flow.io.Storages
import org.oxygenium.flow.setting.{OxygeniumConfig, Configs, Platform}
import org.oxygenium.io.RocksDBSource.ProdSettings
import org.oxygenium.protocol.ALPH
import org.oxygenium.protocol.mining.HashRate
import org.oxygenium.protocol.model.{BlockDeps, Target}
import org.oxygenium.protocol.vm.LockupScript
import org.oxygenium.util.{Env, Math}

object ValidateDifficultyBombPatch extends App {
  private val rootPath: Path = Platform.getRootPath()
  private val typesafeConfig = Configs.parseConfigAndValidate(Env.Prod, rootPath, overwrite = true)
  implicit private val config: OxygeniumConfig = OxygeniumConfig.load(typesafeConfig, "oxygenium")
  private val dbPath                          = rootPath.resolve("mainnet")
  private val storages =
    Storages.createUnsafe(dbPath, "db", ProdSettings.writeOptions)(config.broker, config.node)
  private val blockFlow = BlockFlow.fromStorageUnsafe(config, storages)

  config.broker.chainIndexes.foreach { chainIndex =>
    val chain          = blockFlow.getBlockChain(chainIndex)
    val (_, publicKey) = chainIndex.from.generateKey(config.broker)
    val miner          = LockupScript.p2pkh(publicKey)
    val template       = blockFlow.prepareBlockFlowUnsafe(chainIndex, miner)
    val parent         = BlockDeps.build(template.deps)(config.broker).uncleHash(chainIndex.to)
    val height         = chain.getHeightUnsafe(parent) - ALPH.DifficultyBombPatchHeightDiff
    val target         = chain.getBlockUnsafe(chain.getHashesUnsafe(height).head).target
    val depTargets =
      template.deps.map(hash => blockFlow.getHeaderChain(hash).getBlockHeaderUnsafe(hash).target)
    val weightedTarget = Target.average(target, depTargets)(config.broker)
    val expectecTarget =
      Target.clipByTwoTimes(depTargets.fold(weightedTarget)(Math.max), weightedTarget)

    val blockTargetTime  = config.consensus.mainnet.blockTargetTime
    val hashrate         = HashRate.from(template.target, blockTargetTime)(config.broker).MHs
    val expectedHashRate = HashRate.from(expectecTarget, blockTargetTime)(config.broker).MHs
    if (expectecTarget != template.target) {
      throw new RuntimeException(
        s"ChainIndex: $chainIndex, parent: ${parent.toHexString}, expected: $expectedHashRate, have: $hashrate"
      )
    } else {
      print(s"Succeeded ${hashrate}!\n")
    }
  }
}
