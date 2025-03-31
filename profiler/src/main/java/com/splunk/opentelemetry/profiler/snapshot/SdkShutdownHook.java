package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

class SdkShutdownHook implements SpanProcessor {
  private final List<Closeable> closeables = new ArrayList<>();

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {}

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public CompletableResultCode shutdown() {
    List<CompletableResultCode> results = new ArrayList<>();
    for (Closeable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception e) {
        results.add(CompletableResultCode.ofExceptionalFailure(e));
      }
    }
    return CompletableResultCode.ofAll(results);
  }

  void add(Closeable closeable) {
    closeables.add(closeable);
  }
}
