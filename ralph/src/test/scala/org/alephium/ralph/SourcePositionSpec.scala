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
package org.oxygenium.ralph

import org.oxygenium.util.OxygeniumSpec

class SourcePositionSpec extends OxygeniumSpec {

  it should "parse valid line number format" in {
    forAll { (rowNum: Int, colNum: Int) =>
      SourcePosition.parse(s"$rowNum:$colNum") is SourcePosition(rowNum, colNum)
    }
  }

  it should "report invalid line number formats" in {
    def runCheck(input: String) =
      intercept[Compiler.Error](SourcePosition.parse(input)).message is
        SourcePosition.unsupportedLineNumberFormat(input)

    runCheck("")
    runCheck("12345")
    runCheck("12345:")
    runCheck(":12345")

    forAll(runCheck)
  }
}
