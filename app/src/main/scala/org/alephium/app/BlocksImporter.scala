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

package org.oxygenium.app

import java.io.File

import scala.annotation.tailrec
import scala.io.Source
import scala.util.{Failure, Success, Using}

import com.typesafe.scalalogging.StrictLogging

import org.oxygenium.flow.client.Node
import org.oxygenium.flow.handler.DependencyHandler
import org.oxygenium.flow.model.DataOrigin
import org.oxygenium.io.{IOError, IOResult}
import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.protocol.model.Block
import org.oxygenium.serde.deserialize
import org.oxygenium.util._

object BlocksImporter extends StrictLogging {

  // scalastyle:off magic.number
  private val batchNumber = 100
  // scalastyle:on magic.number

  def importBlocks(file: File, node: Node)(implicit config: GroupConfig): IOResult[Int] = {
    Using(Source.fromFile(file)("UTF-8")) { source =>
      val (genesis, rest) = source.getLines().splitAt(config.chainNum)

      for {
        _           <- validateGenesis(genesis, node)
        blocksCount <- handleRawBlocksIterator(rest, node)
      } yield blocksCount

    } match {
      case Success(blocksCount) => blocksCount
      case Failure(error)       => Left(IOError.Other(error))
    }
  }

  private def validateGenesis(rawGenesises: Iterator[String], node: Node): IOResult[Unit] = {
    val genesis: Set[Block] = rawGenesises.flatMap { rawGenesis =>
      deserialize[Block](Hex.unsafe(rawGenesis.stripLineEnd)).toOption
    }.toSet

    Either.cond(
      node.config.genesisBlocks.flatMap(_.map(_.hash)).toSet == genesis.map(_.hash),
      (),
      IOError.Other(new Throwable("Invalid genesis blocks"))
    )
  }

  private def handleRawBlocksIterator(it: Iterator[String], node: Node): IOResult[Int] = {
    @tailrec
    def rec(sum: Int, groupedIt: Iterator[Seq[String]]): Either[String, Int] = {
      if (groupedIt.hasNext) {
        handleRawBlocks(AVector.from(groupedIt.next()), node) match {
          case Right(number) => rec(sum + number, groupedIt)
          case Left(error)   => Left(error)
        }
      } else {
        Right(sum)
      }
    }

    rec(0, it.grouped(batchNumber)).left.map(error => IOError.Other(new Throwable(error)))
  }

  private def handleRawBlocks(rawBlocks: AVector[String], node: Node): Either[String, Int] = {
    for {
      blocks <- rawBlocks
        .mapE(line => deserialize[Block](Hex.unsafe(line.stripLineEnd)))
        .left
        .map(_.getMessage)
      _ <- validateAndSendBlocks(blocks, node)
    } yield blocks.length
  }

  @SuppressWarnings(Array("org.wartremover.warts.IterableOps"))
  private def validateAndSendBlocks(blocks: AVector[Block], node: Node): Either[String, Unit] = {
    val message = DependencyHandler.AddFlowData(blocks, DataOrigin.Local)
    Right(node.allHandlers.dependencyHandler ! message)
  }
}
