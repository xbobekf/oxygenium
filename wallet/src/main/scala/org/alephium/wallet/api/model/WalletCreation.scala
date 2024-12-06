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

import org.oxygenium.crypto.wallet.Mnemonic

@SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
final case class WalletCreation(
    password: String,
    walletName: String,
    isMiner: Option[Boolean] = None,
    mnemonicPassphrase: Option[String] = None,
    mnemonicSize: Option[Mnemonic.Size] = None
)

final case class WalletCreationResult(walletName: String, mnemonic: Mnemonic)
