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

package org.oxygenium.flow.network.broker

import org.scalatest.concurrent.Eventually

import org.oxygenium.flow.setting.{OxygeniumConfigFixture, NetworkSetting}
import org.oxygenium.util.{discard, OxygeniumSpec, Duration, Math}

class BackoffStrategySpec extends OxygeniumSpec with OxygeniumConfigFixture {
  implicit lazy val network: NetworkSetting    = networkConfig
  def createStrategy(): DefaultBackoffStrategy = DefaultBackoffStrategy()

  it should "calculate the correct delay" in new DefaultFixture {
    var backoff = strategy.baseDelay.divUnsafe(2)
    def test(expected: Duration) = {
      strategy.retry(backoff = _) is true
      backoff is expected
    }

    (0 until BackoffStrategy.maxRetry).foreach { _ =>
      val expected =
        Math.min(backoff.timesUnsafe(2), strategy.maxDelay)
      test(expected)
    }
  }

  it should "not retry more than 1 minute" in new DefaultFixture {
    var total = Duration.zero
    (0 until BackoffStrategy.maxRetry).foreach { _ =>
      strategy.retry(backoff => total = total + backoff)
    }
    (total < Duration.ofMinutesUnsafe(1)) is true
  }

  trait DefaultFixture {
    val strategy: DefaultBackoffStrategy = createStrategy()
  }
}

class ResetBackoffStrategySpec extends BackoffStrategySpec {
  override def createStrategy(): ResetBackoffStrategy = ResetBackoffStrategy()
  override val configValues: Map[String, Any] = Map(
    "oxygenium.network.backoff-reset-delay" -> "50 milli"
  )

  trait ResetFixture extends Eventually {
    val strategy: ResetBackoffStrategy = createStrategy()
  }

  it should "correctly reset the counter" in new ResetFixture {
    (0 until BackoffStrategy.maxRetry).foreach { _ => strategy.retry(discard) is true }
    strategy.retry(discard) is false
    eventually(strategy.retry(discard) is true)
  }
}
