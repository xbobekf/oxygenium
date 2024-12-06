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

package org.oxygenium.protocol.config

import org.oxygenium.protocol.mining.Emission
import org.oxygenium.protocol.model.Target
import org.oxygenium.util.Duration

trait ConsensusConfigsFixture {
  def groupConfig: GroupConfig

  def consensusConfigs: ConsensusConfigs
}

object ConsensusConfigsFixture {
  trait Default extends ConsensusConfigsFixture with GroupConfigFixture.Default {
    implicit lazy val consensusConfigs: ConsensusConfigs = new ConsensusConfigs {
      val mainnet: ConsensusConfig = new ConsensusConfig {
        val maxMiningTarget: Target          = Target.Max
        val blockTargetTime: Duration        = Duration.ofSecondsUnsafe(64)
        val uncleDependencyGapTime: Duration = blockTargetTime
        val emission: Emission               = Emission.mainnet(groupConfig, blockTargetTime)
      }
      val rhone: ConsensusConfig = new ConsensusConfig {
        val maxMiningTarget: Target          = Target.Max
        val blockTargetTime: Duration        = Duration.ofSecondsUnsafe(16)
        val uncleDependencyGapTime: Duration = blockTargetTime
        val emission: Emission =
          Emission.rhone(groupConfig, mainnet.blockTargetTime, blockTargetTime)
      }
    }
  }
}
