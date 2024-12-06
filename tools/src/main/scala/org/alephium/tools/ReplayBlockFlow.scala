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

import java.nio.file.{Files, StandardCopyOption}

import com.typesafe.scalalogging.StrictLogging

import org.oxygenium.flow.client.Node
import org.oxygenium.flow.core.BlockFlow
import org.oxygenium.flow.setting.Platform
import org.oxygenium.flow.validation.{BlockValidation, BlockValidationResult}
import org.oxygenium.io.{IOResult, IOUtils}
import org.oxygenium.util.{Files => AFiles, TimeStamp}

class ReplayBlockFlow(
    val sourceBlockFlow: BlockFlow,
    val targetBlockFlow: BlockFlow
) extends ReplayState
    with StrictLogging {
  private val validator = BlockValidation.build(targetBlockFlow)

  def start(): BlockValidationResult[Boolean] = {
    for {
      _      <- from(init())
      _      <- replay()
      isSame <- from(isStateHashesSame)
    } yield isSame
  }

  private def replay(): BlockValidationResult[Unit] = {
    var result = Right(()): BlockValidationResult[Unit]

    while (blockQueue.nonEmpty && result.isRight) {
      val (block, blockHeight) = blockQueue.dequeue()
      val chainIndex           = block.chainIndex

      var endValidationTs = TimeStamp.zero
      result = for {
        sideEffect <- validator.validate(block, targetBlockFlow)
        _ <- from(targetBlockFlow.add(block, sideEffect)).map(_ =>
          endValidationTs = TimeStamp.now()
        )
        _ <- from(IOUtils.tryExecute(loadMoreBlocksUnsafe(chainIndex, blockHeight)))
      } yield ()

      replayedBlockCount += 1
      if (replayedBlockCount % ReplayState.LogInterval == 0) {
        val (speed, cycleSpeed) = calcSpeed()
        print(s"Replayed #$replayedBlockCount blocks, #$speed BPS, #$cycleSpeed cycle BPS\n")
      }
    }

    result
  }

  private def from[T](result: IOResult[T]): BlockValidationResult[T] = {
    result.left.map(Left(_))
  }
}

object ReplayBlockFlow extends App with StrictLogging {
  private val sourcePath = Platform.getRootPath()
  private val targetPath = {
    val path = AFiles.homeDir.resolve(".oxygenium-replay")
    path.toFile.mkdir()
    Files.copy(
      sourcePath.resolve("user.conf"),
      path.resolve("user.conf"),
      StandardCopyOption.REPLACE_EXISTING
    )
    path
  }

  private val (sourceBlockFlow, sourceStorages) = Node.buildBlockFlowUnsafe(sourcePath)
  private val (targetBlockFlow, targetStorages) = Node.buildBlockFlowUnsafe(targetPath)

  Runtime.getRuntime.addShutdownHook(new Thread(() => {
    sourceStorages.closeUnsafe()
    targetStorages.closeUnsafe()
  }))

  new ReplayBlockFlow(sourceBlockFlow, targetBlockFlow).start() match {
    case Right(valid) =>
      if (valid) {
        print("Replay blocks succeeded\n")
      } else {
        logger.error("All blocks replayed, but state hashes do not match")
      }
    case Left(error) =>
      logger.error(s"Replay blocks failed: $error")
  }
}
