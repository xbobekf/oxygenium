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

package org.alephium.flow.model

import org.alephium.protocol.model.{BrokerInfo, CliqueId}

sealed trait DataOrigin {
  def isLocal: Boolean

  def isFrom(another: CliqueId): Boolean

  def isFrom(brokerInfo: BrokerInfo): Boolean
}

object DataOrigin {
  case object Local extends DataOrigin {
    def isLocal: Boolean = true

    def isFrom(another: CliqueId): Boolean = false

    def isFrom(brokerInfo: BrokerInfo): Boolean = false
  }

  sealed trait FromClique extends DataOrigin {
    def isLocal: Boolean = false

    def brokerInfo: BrokerInfo

    def cliqueId: CliqueId = brokerInfo.cliqueId

    def isFrom(another: CliqueId): Boolean = cliqueId == another

    def isFrom(_brokerInfo: BrokerInfo): Boolean = _brokerInfo == brokerInfo
  }
  final case class InterClique(brokerInfo: BrokerInfo) extends FromClique
  final case class IntraClique(brokerInfo: BrokerInfo) extends FromClique
}
