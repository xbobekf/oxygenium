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

package org.oxygenium.wallet.api.model

import org.oxygenium.crypto.wallet.BIP32.ExtendedPrivateKey
import org.oxygenium.protocol.config.GroupConfig
import org.oxygenium.protocol.model.Address
import org.oxygenium.util.AVector

final case class Addresses(activeAddress: Address.Asset, addresses: AVector[AddressInfo])

object Addresses {
  def from(
      activeKey: ExtendedPrivateKey,
      allPrivateKeys: AVector[ExtendedPrivateKey]
  )(implicit config: GroupConfig): Addresses = {
    Addresses(Address.p2pkh(activeKey.publicKey), allPrivateKeys.map(AddressInfo.from))
  }
}
