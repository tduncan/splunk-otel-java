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

import java.util.Objects;
import java.util.function.Supplier;

class StackTraceSamplerProvider implements Supplier<StackTraceSampler> {
  public static final StackTraceSamplerProvider INSTANCE = new StackTraceSamplerProvider();

  private StackTraceSampler sampler = StackTraceSampler.NOOP;

  @Override
  public StackTraceSampler get() {
    return sampler;
  }

  void configure(StackTraceSampler sampler) {
    this.sampler = Objects.requireNonNull(sampler);
  }

  private StackTraceSamplerProvider() {}
}
