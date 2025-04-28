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

import io.opentelemetry.api.trace.SpanContext;
import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class TraceRegistry implements Closeable {
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final Map<String, RegistrationContext> traceIds = new ConcurrentHashMap<>();
  private final Supplier<TraceRegistrationNotifier> notifier;

  TraceRegistry(Supplier<TraceRegistrationNotifier> notifier) {
    this(notifier, Duration.ofSeconds(10));
  }

  TraceRegistry(Supplier<TraceRegistrationNotifier> notifier, Duration stalledTimeLimit) {
    this.notifier = notifier;
    scheduler.scheduleAtFixedRate(
        removeStalledTraces(stalledTimeLimit),
        0,
        stalledTimeLimit.toMillis() / 2,
        TimeUnit.MILLISECONDS);
  }

  void register(SpanContext spanContext) {
    traceIds.put(spanContext.getTraceId(), new RegistrationContext(Instant.now(), spanContext));
    notifier.get().traceRegistered(spanContext);
  }

  boolean isRegistered(SpanContext spanContext) {
    return traceIds.containsKey(spanContext.getTraceId());
  }

  void unregister(SpanContext spanContext) {
    traceIds.remove(spanContext.getTraceId());
    notifier.get().traceUnregistered(spanContext);
  }

  private Runnable removeStalledTraces(Duration stalledTimeLimit) {
    return () ->
        traceIds
            .entrySet()
            .iterator()
            .forEachRemaining(
                entry -> {
                  Instant now = Instant.now();
                  RegistrationContext context = entry.getValue();
                  Duration duration = Duration.between(now, context.registrationTime);
                  if (duration.compareTo(stalledTimeLimit) <= 0) {
                    unregister(context.spanContext);
                  }
                });
  }

  @Override
  public void close() {
    scheduler.shutdown();
  }

  private static class RegistrationContext {
    private final Instant registrationTime;
    private final SpanContext spanContext;

    private RegistrationContext(Instant registrationTime, SpanContext spanContext) {
      this.registrationTime = registrationTime;
      this.spanContext = spanContext;
    }
  }
}
