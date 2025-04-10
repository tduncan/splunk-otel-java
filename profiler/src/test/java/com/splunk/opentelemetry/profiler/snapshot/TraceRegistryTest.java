/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TraceRegistryTest {
  private final TraceRegistry registry = new TraceRegistry(() -> new  TraceRegistrationNotifier(() -> StackTraceSampler.NOOP));

  @AfterEach
  void teardown() {
    registry.close();
  }

  @Test
  void registerTrace() {
    var spanContext = Snapshotting.spanContext().build();
    registry.register(spanContext);
    assertThat(registry.isRegistered(spanContext)).isTrue();
  }

  @Test
  void unregisteredTracesAreNotRegisteredForProfiling() {
    var spanContext = Snapshotting.spanContext().build();
    assertThat(registry.isRegistered(spanContext)).isFalse();
  }

  @Test
  void unregisterTraceForProfiling() {
    var spanContext = Snapshotting.spanContext().build();

    registry.register(spanContext);
    registry.unregister(spanContext);

    assertThat(registry.isRegistered(spanContext)).isFalse();
  }

  @Test
  void removeRegisteredTracesAfterStalledTimeLimitPasses() {
    var spanContext = Snapshotting.spanContext().build();
    var stalledTimeLimit = Duration.ofMillis(10);
    var sampler = new ObservableStackTraceSampler();
    var notifier = new TraceRegistrationNotifier(() -> sampler);

    try (var registry = new TraceRegistry(() -> notifier, stalledTimeLimit)) {
      registry.register(spanContext);
      assertTrue(registry.isRegistered(spanContext));

      await().untilAsserted(() -> assertThat(registry.isRegistered(spanContext)).isFalse());
    }
  }

  @Test
  void sendNotificationWhenTraceIsRegistered() {
    var spanContext = Snapshotting.spanContext().build();
    var sampler = new ObservableStackTraceSampler();
    var notifier = new TraceRegistrationNotifier(() -> sampler);

    try (var registry = new TraceRegistry(() -> notifier)) {
      registry.register(spanContext);
      assertThat(sampler.isBeingSampled(spanContext)).isTrue();
    }
  }

  @Test
  void sendNotificationWhenTraceIsUnregistered() {
    var spanContext = Snapshotting.spanContext().build();
    var sampler = new ObservableStackTraceSampler();
    var notifier = new TraceRegistrationNotifier(() -> sampler);
    var stalledTimeLimit = Duration.ofMillis(10);

    try (var registry = new TraceRegistry(() -> notifier, stalledTimeLimit)) {
      registry.register(spanContext);
      await().untilAsserted(() -> assertThat(sampler.isBeingSampled(spanContext)).isFalse());
    }
  }
}
