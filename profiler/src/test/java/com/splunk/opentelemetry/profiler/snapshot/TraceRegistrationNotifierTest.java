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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceRegistrationNotifierTest {
  private final ObservableStackTraceSampler sampler = new ObservableStackTraceSampler();
  private final TraceRegistrationNotifier notifier =
      new TraceRegistrationNotifier(StackTraceSampler.SUPPLIER);

  @BeforeEach
  void setup() {
    StackTraceSampler.SUPPLIER.configure(sampler);
  }

  @AfterEach
  void teardown() {
    StackTraceSampler.SUPPLIER.reset();
  }

  @Test
  void notifyStackTraceSamplerWhenTraceIsRegistered() {
    var spanContext = Snapshotting.spanContext().build();
    notifier.traceRegistered(spanContext);
    assertThat(sampler.isBeingSampled(spanContext)).isTrue();
  }

  @Test
  void notifyStackTraceSamplerWhenTraceIsUnregistered() {
    var spanContext = Snapshotting.spanContext().build();
    notifier.traceRegistered(spanContext);
    notifier.traceUnregistered(spanContext);
    assertThat(sampler.isBeingSampled(spanContext)).isFalse();
  }
}
