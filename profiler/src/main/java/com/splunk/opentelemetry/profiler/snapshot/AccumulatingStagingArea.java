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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

class AccumulatingStagingArea implements StagingArea {
  private final ConcurrentMap<String, List<StackTrace>> stackTraces = new ConcurrentHashMap<>();
  private final Supplier<StackTraceExporter> exporter;

  AccumulatingStagingArea(Supplier<StackTraceExporter> exporter) {
    this.exporter = exporter;
  }

  @Override
  public void stage(String traceId, StackTrace stackTrace) {
    stackTraces.compute(
        traceId,
        (id, stackTraces) -> {
          if (stackTraces == null) {
            stackTraces = new ArrayList<>();
          }
          stackTraces.add(stackTrace);
          return stackTraces;
        });
  }

  @Override
  public void empty(String traceId) {
    List<StackTrace> stackTraces = this.stackTraces.remove(traceId);
    if (stackTraces != null) {
      exporter.get().export(stackTraces);
    }
  }
}
