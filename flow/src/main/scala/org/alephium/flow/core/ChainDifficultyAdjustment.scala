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

package org.oxygenium.flow.core

import java.math.BigInteger

import org.oxygenium.flow.setting.ConsensusSetting
import org.oxygenium.io.IOResult
import org.oxygenium.protocol.OXYG
import org.oxygenium.protocol.config.NetworkConfig
import org.oxygenium.protocol.model.{BlockHash, Target}
import org.oxygenium.util.{AVector, Duration, TimeStamp}

trait ChainDifficultyAdjustment {
  implicit def networkConfig: NetworkConfig

  val difficultyBombPatchConfig =
    new ChainDifficultyAdjustment.DifficultyBombPatchConfig {
      val enabledTimeStamp: TimeStamp = OXYG.DifficultyBombPatchEnabledTimeStamp
      val heightDiff: Int             = OXYG.DifficultyBombPatchHeightDiff
    }

  def getHeight(hash: BlockHash): IOResult[Int]

  def getTimestamp(hash: BlockHash): IOResult[TimeStamp]

  def chainBackUntil(hash: BlockHash, heightUntil: Int): IOResult[AVector[BlockHash]]

  def getTarget(height: Int): IOResult[Target]

  // TODO: optimize this
  final protected[core] def calTimeSpan(
      hash: BlockHash,
      height: Int,
      nextTimeStamp: TimeStamp
  )(implicit consensusConfig: ConsensusSetting): IOResult[Option[Duration]] = {
    val earlyHeight = height - consensusConfig.powAveragingWindow - 1
    assume(earlyHeight >= OXYG.GenesisHeight)
    calTimeSpan(hash, height).map { case (timestampLast, timestampNow) =>
      if (
        timestampLast < difficultyBombPatchConfig.enabledTimeStamp &&
        difficultyBombPatchConfig.enabledTimeStamp <= nextTimeStamp
      ) {
        None
      } else {
        Some(timestampNow.deltaUnsafe(timestampLast))
      }
    }
  }

  final protected[core] def calTimeSpan(
      hash: BlockHash,
      height: Int
  )(implicit consensusConfig: ConsensusSetting): IOResult[(TimeStamp, TimeStamp)] = {
    val earlyHeight = height - consensusConfig.powAveragingWindow - 1
    assume(earlyHeight >= OXYG.GenesisHeight)
    for {
      hashes        <- chainBackUntil(hash, earlyHeight)
      timestampNow  <- getTimestamp(hash)
      timestampLast <- getTimestamp(hashes.head)
    } yield (timestampLast, timestampNow)
  }

  final def calIceAgeTarget(
      currentTarget: Target,
      currentTimeStamp: TimeStamp,
      nextTimeStamp: TimeStamp
  ): Target = {
    val hardFork = networkConfig.getHardFork(currentTimeStamp)
    if (hardFork.isLemanEnabled() || OXYG.DifficultyBombPatchEnabledTimeStamp <= nextTimeStamp) {
      currentTarget
    } else {
      _calIceAgeTarget(currentTarget, currentTimeStamp, OXYG.PreLemanDifficultyBombEnabledTimestamp)
    }
  }

  final def _calIceAgeTarget(
      currentTarget: Target,
      timestamp: TimeStamp,
      difficultyBombEnabledTimestamp: TimeStamp
  ): Target = {
    if (timestamp.isBefore(difficultyBombEnabledTimestamp)) {
      currentTarget
    } else {
      val periodCount = timestamp
        .deltaUnsafe(difficultyBombEnabledTimestamp)
        .millis / OXYG.ExpDiffPeriod.millis
      Target.unsafe(currentTarget.value.shiftRight(periodCount.toInt))
    }
  }

  // DigiShield DAA V3 variant
  final protected[core] def calNextHashTargetRaw(
      hash: BlockHash,
      currentTarget: Target,
      currentTimeStamp: TimeStamp,
      nextTimeStamp: TimeStamp
  )(implicit consensusConfig: ConsensusSetting): IOResult[Target] = {
    getHeight(hash).flatMap {
      case height if ChainDifficultyAdjustment.enoughHeight(height) =>
        calTimeSpan(hash, height, nextTimeStamp).flatMap {
          case Some(timeSpan) =>
            val target = ChainDifficultyAdjustment.calNextHashTargetRaw(
              currentTarget,
              timeSpan
            )
            Right(calIceAgeTarget(target, currentTimeStamp, nextTimeStamp))
          case None =>
            getTarget(height - difficultyBombPatchConfig.heightDiff)
        }
      case _ => Right(currentTarget)
    }
  }
}

object ChainDifficultyAdjustment {
  trait DifficultyBombPatchConfig {
    def enabledTimeStamp: TimeStamp
    def heightDiff: Int
  }

  def enoughHeight(height: Int)(implicit consensusConfig: ConsensusSetting): Boolean = {
    height >= OXYG.GenesisHeight + consensusConfig.powAveragingWindow + 1
  }

  def calNextHashTargetRaw(
      currentTarget: Target,
      lastWindowTimeSpan: Duration
  )(implicit consensusConfig: ConsensusSetting): Target = {
    var clippedTimeSpan =
      consensusConfig.expectedWindowTimeSpan.millis + (lastWindowTimeSpan.millis - consensusConfig.expectedWindowTimeSpan.millis) / 4
    if (clippedTimeSpan < consensusConfig.windowTimeSpanMin.millis) {
      clippedTimeSpan = consensusConfig.windowTimeSpanMin.millis
    } else if (clippedTimeSpan > consensusConfig.windowTimeSpanMax.millis) {
      clippedTimeSpan = consensusConfig.windowTimeSpanMax.millis
    }
    reTarget(currentTarget, clippedTimeSpan)
  }

  @inline def reTarget(currentTarget: Target, timeSpanMs: Long)(implicit
      consensusConfig: ConsensusSetting
  ): Target = {
    val nextTarget = currentTarget.value
      .multiply(BigInteger.valueOf(timeSpanMs))
      .divide(BigInteger.valueOf(consensusConfig.expectedWindowTimeSpan.millis))
    if (nextTarget.compareTo(consensusConfig.maxMiningTarget.value) <= 0) {
      Target.unsafe(nextTarget)
    } else {
      consensusConfig.maxMiningTarget
    }
  }
}
