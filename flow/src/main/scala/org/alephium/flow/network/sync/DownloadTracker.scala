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

package org.oxygenium.flow.network.sync

import scala.collection.mutable

import org.oxygenium.flow.core.BlockFlow
import org.oxygenium.flow.network.broker.BrokerHandler
import org.oxygenium.protocol.model.BlockHash
import org.oxygenium.util.{AVector, BaseActor, Duration, TimeStamp}

trait DownloadTracker extends BaseActor {
  def blockflow: BlockFlow

  val syncing: mutable.HashMap[BlockHash, TimeStamp] = mutable.HashMap.empty

  def needToDownload(hash: BlockHash): Boolean =
    !(syncing.contains(hash) || blockflow.containsUnsafe(hash))

  def download(hashes: AVector[AVector[BlockHash]]): Unit = {
    val currentTs  = TimeStamp.now()
    val toDownload = hashes.flatMap(_.filter(needToDownload))
    toDownload.foreach(hash => syncing.addOne(hash -> currentTs))
    sender() ! BrokerHandler.DownloadBlocks(toDownload)
  }

  def finalized(hash: BlockHash): Unit = {
    syncing -= hash
  }

  def cleanupSyncing(aliveDuration: Duration): Unit = {
    val threshold = TimeStamp.now().minusUnsafe(aliveDuration)
    val oldSize   = syncing.size
    syncing.filterInPlace { case (_, timestamp) =>
      timestamp > threshold
    }
    val newSize   = syncing.size
    val sizeDelta = oldSize - newSize
    if (sizeDelta > 0) {
      log.debug(s"Clean up #$sizeDelta hashes from syncing pool")
    }
  }
}
