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

import org.oxygenium.flow.FlowFixture
import org.oxygenium.io.IOError
import org.oxygenium.util.OxygeniumSpec

class BlocksExporterSpec extends OxygeniumSpec {

  it should "validate filename" in new Fixture {
    Seq(
      "wrong?file",
      "wrong.name!",
      "wrong#name",
      "wrong+name",
      "",
      ".",
      ".."
    ).foreach { filename => blocksExporter.export(filename).leftValue is a[IOError] }

    Seq(
      "correct-file",
      "correct_file",
      "correct.file",
      "correctfile._",
      "correct.file."
    ).foreach { filename => blocksExporter.export(filename).rightValue is () }
  }

  trait Fixture extends FlowFixture {

    override val configValues: Map[String, Any] = Map(
      ("oxygenium.broker.broker-num", 1)
    )

    val blocksExporter: BlocksExporter = new BlocksExporter(blockFlow, rootPath)
  }
}
