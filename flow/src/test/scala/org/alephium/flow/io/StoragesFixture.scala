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

package org.oxygenium.flow.io

import java.nio.file.{Files, Path}

import org.oxygenium.flow.setting.NodeSetting
import org.oxygenium.io.RocksDBSource
import org.oxygenium.protocol.Hash
import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.util.OxygeniumSpec

trait StoragesFixture {
  def storages: Storages

  def cleanStorages(): Unit = {
    storages.dESTROYUnsafe()
  }
}

object StoragesFixture {
  def buildStorages(
      rootPath: Path
  )(implicit groupConfig: GroupConfig, nodeSetting: NodeSetting): Storages = {
    if (!Files.exists(rootPath)) rootPath.toFile.mkdir()

    val postFix   = Hash.random.toHexString
    val dbFolders = s"db-$postFix"
    val storages: Storages =
      Storages.createUnsafe(rootPath, dbFolders, RocksDBSource.ProdSettings.syncWrite)
    OxygeniumSpec.addCleanTask(() => storages.dESTROYUnsafe())
    storages
  }

  trait Default extends StoragesFixture {
    def rootPath: Path
    implicit def groupConfig: GroupConfig
    implicit def nodeSetting: NodeSetting

    lazy val storages = StoragesFixture.buildStorages(rootPath)
  }
}
