package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceRegistrationNotifierTest {
  private final ObservableStackTraceSampler sampler = new ObservableStackTraceSampler();
  private final TraceRegistrationNotifier notifier = new TraceRegistrationNotifier(StackTraceSamplerProvider.INSTANCE);

  @BeforeEach
  void setup() {
    StackTraceSamplerProvider.INSTANCE.configure(sampler);
  }

  @AfterEach
  void teardown() {
    StackTraceSamplerProvider.INSTANCE.configure(StackTraceSampler.NOOP);
  }

  @Test
  void notifyStackTraceSamplerWhenTraceIsRegistered() {
    var spanContext = Snapshotting.spanContext().build();
    notifier.traceRegistered(spanContext.getTraceId());
    assertThat(sampler.isBeingSampled(spanContext)).isTrue();
  }

  @Test
  void notifyStackTraceSamplerWhenTraceIsUnregistered() {
    var spanContext = Snapshotting.spanContext().build();
    notifier.traceRegistered(spanContext.getTraceId());
    notifier.traceUnregistered(spanContext.getTraceId());
    assertThat(sampler.isBeingSampled(spanContext)).isFalse();
  }
}
