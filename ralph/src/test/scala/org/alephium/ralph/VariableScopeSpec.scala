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

class VariableScopeSpec extends OxygeniumSpec {
  it should "check variable scope" in {
    val ref0 = Ast.Ident("0")
    val ref1 = Ast.Ident("1")
    val ref2 = Ast.Ident("2")

    val scope0    = ChildScope(FunctionRoot, ref0, None, 1)
    val scope1    = ChildScope(scope0, ref1, None, 2)
    val scope2    = ChildScope(scope0, ref2, None, 2)
    val allScopes = Seq(FunctionRoot, scope0, scope1, scope2)

    allScopes.foreach { scope =>
      FunctionRoot.include(scope) is true
      if (scope != FunctionRoot) {
        scope.include(FunctionRoot) is false
      }
      scope.include(scope) is true
    }

    scope0.include(scope1) is true
    scope0.include(scope2) is true
    scope1.include(scope2) is false
    scope2.include(scope1) is false
  }
}
