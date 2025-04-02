package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilerActivatorTest {
  @AfterEach
  void tearDown() {
    StagingAreaProvider.INSTANCE.reset();
    StackTraceExporterProvider.INSTANCE.reset();
    CloserProvider.INSTANCE.reset();
  }

  @Nested
  class SnapshotProfilingEnabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.configure()
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .with(new SnapshotProfilerActivator())
            .build();

    @Test
    void configureStagingAreaProvider() {
      var stagingArea = StagingAreaProvider.INSTANCE.get();
      assertNotSame(StagingArea.NOOP, stagingArea);
      assertInstanceOf(PeriodicallyExportingStagingArea.class, stagingArea);
    }

    @Test
    void configureStackTraceExporterProvider() {
      var exporter = StackTraceExporterProvider.INSTANCE.get();
      assertNotSame(StackTraceExporter.NOOP, exporter);
      assertInstanceOf(AsyncStackTraceExporter.class, exporter);
    }

    @Test
    void configureCloserProvider() {
      var closer = CloserProvider.INSTANCE.get();
      assertNotSame(Closer.NOOP, closer);
      assertInstanceOf(Closer.class, closer);
    }
  }

  @Nested
  class SnapshotProfilingDisabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.configure()
            .withProperty("splunk.snapshot.profiler.enabled", "false")
            .with(new StackTraceExporterActivator())
            .build();

    @Test
    void doNotConfigureStagingAreaProvider() {
      var stagingArea = StagingAreaProvider.INSTANCE.get();
      assertSame(StagingArea.NOOP, stagingArea);
    }

    @Test
    void doNotConfigureStackTraceExporterProvider() {
      var exporter = StackTraceExporterProvider.INSTANCE.get();
      assertSame(StackTraceExporter.NOOP, exporter);
    }

    @Test
    void doNotConfigureCloserProvider() {
      var closer = CloserProvider.INSTANCE.get();
      assertSame(Closer.NOOP, closer);
    }
  }
}
