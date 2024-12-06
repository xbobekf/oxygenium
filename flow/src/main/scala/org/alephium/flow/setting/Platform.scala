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

package org.oxygenium.flow.setting

import java.nio.file.{Files => JFiles, Path, Paths}

import com.typesafe.scalalogging.StrictLogging

import org.oxygenium.util.{Env, Files}

object Platform extends StrictLogging {
  def getRootPath(): Path = getRootPath(Env.currentEnv)

  def getRootPath(env: Env): Path = {
    val rootPath = env match {
      case Env.Prod =>
        sys.env.get("OXYGENIUM_HOME") match {
          case Some(rawPath) => Paths.get(rawPath)
          case None          => Files.homeDir.resolve(".oxygenium")
        }
      case Env.Debug =>
        Files.homeDir.resolve(s".oxygenium-${env.name}")
      case env => Files.testRootPath(env)
    }
    if (!JFiles.exists(rootPath)) {
      Env.forProd(logger.info(s"Creating root path: $rootPath"))
      rootPath.toFile.mkdir()
    }
    rootPath
  }
}
