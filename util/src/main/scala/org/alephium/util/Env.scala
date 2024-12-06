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

package org.oxygenium.util

sealed trait Env {
  def name: String
}

object Env {
  case object Prod        extends Env { override def name: String = "prod"  }
  case object Debug       extends Env { override def name: String = "debug" }
  case object Test        extends Env { override def name: String = "test"  }
  case object Integration extends Env { override def name: String = "it"    }

  lazy val currentEnv = this.resolve()

  def resolve(): Env =
    resolve(sys.env.getOrElse("OXYGENIUM_ENV", "prod"))

  def resolve(env: String): Env = {
    env match {
      case "debug" => Debug
      case "test"  => Test
      case "it"    => Integration
      case _       => Prod
    }
  }

  def isTestEnv: Boolean = {
    currentEnv match {
      case Test | Integration => true
      case _                  => false
    }
  }

  def forProd(f: => Unit): Unit = {
    currentEnv match {
      case Prod => f
      case _    => ()
    }
  }
}
