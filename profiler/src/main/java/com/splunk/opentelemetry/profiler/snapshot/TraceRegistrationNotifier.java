package com.splunk.opentelemetry.profiler.snapshot;

import java.util.function.Supplier;

class TraceRegistrationNotifier {
  private final Supplier<StackTraceSampler> sampler;

  TraceRegistrationNotifier(Supplier<StackTraceSampler> sampler) {
    this.sampler = sampler;
  }

  void traceRegistered(String traceId) {
    sampler.get().start(traceId);
  }

  void traceUnregistered(String traceId) {
    sampler.get().stop(traceId);
  }
}
