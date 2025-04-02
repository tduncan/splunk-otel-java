package com.splunk.opentelemetry.profiler.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StagingAreaProviderTest {
  private final StagingAreaProvider provider = StagingAreaProvider.INSTANCE;

  @AfterEach
  void tearDown() {
    provider.reset();
  }

  @Test
  void provideNoopStagingAreaWhenNotConfigured() {
    assertSame(StagingArea.NOOP, provider.get());
  }

  @Test
  void providedConfiguredStagingArea() {
    var stagingArea = new InMemoryStagingArea();
    provider.configure(stagingArea);
    assertSame(stagingArea, provider.get());
  }

  @Test
  void canResetConfiguredStagingArea() {
    provider.reset();
    assertSame(StagingArea.NOOP, provider.get());
  }
}
