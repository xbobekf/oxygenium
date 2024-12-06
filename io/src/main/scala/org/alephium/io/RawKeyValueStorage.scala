// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
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

package org.alephium.io

import akka.util.ByteString

trait RawKeyValueStorage {
  def getRawUnsafe(key: ByteString): ByteString

  def getOptRawUnsafe(key: ByteString): Option[ByteString]

  def multiGetRawUnsafe(keys: Seq[ByteString]): Seq[ByteString]

  def putRawUnsafe(key: ByteString, value: ByteString): Unit

  def putBatchRawUnsafe(f: ((ByteString, ByteString) => Unit) => Unit): Unit

  def existsRawUnsafe(key: ByteString): Boolean

  def deleteRawUnsafe(key: ByteString): Unit

  def deleteBatchRawUnsafe(keys: Seq[ByteString]): Unit
}
