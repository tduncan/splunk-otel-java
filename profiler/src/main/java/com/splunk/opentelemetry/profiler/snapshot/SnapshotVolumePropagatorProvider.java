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

import com.google.auto.service.AutoService;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;

@AutoService(ConfigurablePropagatorProvider.class)
public class SnapshotVolumePropagatorProvider implements ConfigurablePropagatorProvider {
  public static final String NAME = "splunk-snapshot";

  @Override
  public TextMapPropagator getPropagator(ConfigProperties config) {
    double snapshotSelectionRate = SnapshotProfilingConfiguration.getSnapshotSelectionRate(config);
    return new SnapshotVolumePropagator(selector(snapshotSelectionRate));
  }

  private SnapshotSelector selector(double selectionRate) {
    return new TraceIdBasedSnapshotSelector(selectionRate)
        .or(new ProbabilisticSnapshotSelector(selectionRate));
  }

  @Override
  public String getName() {
    return NAME;
  }
}
