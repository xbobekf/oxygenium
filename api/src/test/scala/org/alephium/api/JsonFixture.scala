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

package org.oxygenium.api

import org.scalatest.Assertion

import org.oxygenium.json.Json._
import org.oxygenium.util.{OxygeniumSpec, Duration}

trait JsonFixture extends ApiModelCodec with OxygeniumSpec {

  val blockflowFetchMaxAge = Duration.unsafe(1000)

  def checkData[T: Reader: Writer](
      data: T,
      jsonRaw: String,
      dropWhiteSpace: Boolean = true
  ): Assertion = {
    write(data) is jsonRaw.filterNot(v => dropWhiteSpace && v.isWhitespace)
    read[T](jsonRaw) is data
  }
}
