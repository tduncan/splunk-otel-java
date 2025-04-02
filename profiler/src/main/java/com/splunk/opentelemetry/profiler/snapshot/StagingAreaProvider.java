package com.splunk.opentelemetry.profiler.snapshot;

import java.util.function.Supplier;

class StagingAreaProvider implements Supplier<StagingArea> {
  static final StagingAreaProvider INSTANCE = new StagingAreaProvider();

  private StagingArea stagingArea = StagingArea.NOOP;

  @Override
  public StagingArea get() {
    return stagingArea;
  }

  void configure(StagingArea stagingArea) {
    if (stagingArea != null) {
      this.stagingArea = stagingArea;
    }
  }

  void reset() {
    stagingArea = StagingArea.NOOP;
  }

  private StagingAreaProvider() {}
}
