package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.function.Supplier;

class TraceRegistrationNotifier {
  static final TraceRegistrationNotifier INSTANCE = new TraceRegistrationNotifier(
      StackTraceSamplerProvider.INSTANCE);

  private final Supplier<StackTraceSampler> sampler;

  TraceRegistrationNotifier(Supplier<StackTraceSampler> sampler) {
    this.sampler = sampler;
  }

  void traceRegistered(String traceId) {
    SpanContext spanContext = SpanContext.create(traceId, IdGenerator.random().generateSpanId(), TraceFlags.getDefault(), TraceState.getDefault());
    sampler.get().start(spanContext);
  }

  void traceUnregistered(String traceId) {
    SpanContext spanContext = SpanContext.create(traceId, IdGenerator.random().generateSpanId(), TraceFlags.getDefault(), TraceState.getDefault());
    sampler.get().stop(spanContext);
  }
}
