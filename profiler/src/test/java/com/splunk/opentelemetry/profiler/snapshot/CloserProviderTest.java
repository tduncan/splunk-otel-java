package com.splunk.opentelemetry.profiler.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CloserProviderTest {
  private final CloserProvider provider = CloserProvider.INSTANCE;

  @AfterEach
  void tearDown() {
    provider.reset();
  }

  @Test
  void provideNoopExporterWhenNotConfigured() {
    assertSame(Closer.NOOP, provider.get());
  }

  @Test
  void providedConfiguredExporter() {
    var closer = new Closer();
    provider.configure(closer);
    assertSame(closer, provider.get());
  }

  @Test
  void canResetConfiguredExporter() {
    var closer = new Closer();
    provider.reset();
    assertSame(Closer.NOOP, provider.get());
  }
}
