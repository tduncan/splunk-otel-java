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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccumulatingStagingAreaTest {
  private final InMemoryStackTraceExporter exporter = new InMemoryStackTraceExporter();
  private final AccumulatingStagingArea stagingArea = new AccumulatingStagingArea(() -> exporter);

  @Test
  void exportStackTracesToLogExporter() {
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage(stackTrace);
    stagingArea.empty(stackTrace.getTraceId());

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }

  @Test
  void onlyExportStackTracesWhenAtLeastOneHasBeenStaged() {
    var traceId = IdGenerator.random().generateTraceId();
    stagingArea.empty(traceId);
    assertEquals(Collections.emptyList(), exporter.stackTraces());
  }

  @Test
  void exportMultipleStackTracesToLogExporter() {
    var traceId = IdGenerator.random().generateTraceId();
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withTraceId(traceId).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(1).withTraceId(traceId).withName("two").build();

    stagingArea.stage(stackTrace1);
    stagingArea.stage(stackTrace2);
    stagingArea.empty(traceId);

    assertEquals(List.of(stackTrace1, stackTrace2), exporter.stackTraces());
  }

  @Test
  void exportStackTracesForOnlySpecifiedThread() {
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(1).withName("two").build();

    stagingArea.stage(stackTrace1);
    stagingArea.stage(stackTrace2);
    stagingArea.empty(stackTrace1.getTraceId());

    assertEquals(List.of(stackTrace1), exporter.stackTraces());
  }

  @Test
  void exportStackTracesForMultipleThreads() {
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(1).withName("two").build();

    stagingArea.stage(stackTrace1);
    stagingArea.stage(stackTrace2);
    stagingArea.empty(stackTrace1.getTraceId());
    stagingArea.empty(stackTrace2.getTraceId());

    assertEquals(List.of(stackTrace1, stackTrace2), exporter.stackTraces());
  }

  @Test
  void stackTracesAreNotExportedMultipleTimes() {
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage(stackTrace);
    stagingArea.empty(stackTrace.getTraceId());
    stagingArea.empty(stackTrace.getTraceId());

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }
}
