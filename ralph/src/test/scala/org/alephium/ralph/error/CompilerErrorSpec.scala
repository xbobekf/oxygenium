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
package org.oxygenium.ralph.error

import org.scalacheck._

import org.oxygenium.ralph.SourceIndex
import org.oxygenium.util.OxygeniumSpec

class CompilerErrorSpec extends OxygeniumSpec {

  "Default error" should "correctly report sourceIndex" in {
    val message = "error message"

    info("SourceIndex is None")
    val noneError = CompilerError(message, None)
    noneError.position is 0
    noneError.foundLength is 0
    noneError.fileURI is None

    info("SourceIndex is empty")
    val emptyError = CompilerError(message, Some(SourceIndex.empty))
    emptyError.position is 0
    emptyError.foundLength is 0
    emptyError.fileURI is None

    forAll(Gen.posNum[Int], Gen.posNum[Int], Gen.option(Gen.alphaStr)) { case (index, width, uri) =>
      val fileURI     = uri.map(new java.net.URI(_))
      val sourceIndex = SourceIndex(index, width, fileURI)
      val error       = CompilerError(message, Some(sourceIndex))
      error.position is index
      error.foundLength is width
      error.fileURI is fileURI
    }
  }
}
