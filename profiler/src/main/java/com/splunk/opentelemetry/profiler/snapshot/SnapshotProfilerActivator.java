package com.splunk.opentelemetry.profiler.snapshot;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.Configuration;
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.io.Closeable;
import java.time.Duration;

@AutoService(AgentListener.class)
public class SnapshotProfilerActivator implements AgentListener {
  private final OtelLoggerFactory otelLoggerFactory;

  public SnapshotProfilerActivator() {
    this(new OtelLoggerFactory());
  }

  @VisibleForTesting
  SnapshotProfilerActivator(OtelLoggerFactory otelLoggerFactory) {
    this.otelLoggerFactory = otelLoggerFactory;
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties properties = AutoConfigureUtil.getConfig(autoConfiguredOpenTelemetrySdk);
    if (snapshotProfilingEnabled(properties)) {
      Resource resource = AutoConfigureUtil.getResource(autoConfiguredOpenTelemetrySdk);

      activateCloser();
      activateStagingArea(properties);
      activateStackTraceExporting(resource, properties);
    }
  }

  private void activateCloser() {
    CloserProvider.INSTANCE.configure(new Closer());
  }

  private void activateStagingArea(ConfigProperties properties) {
    Duration stagingAreaEmptyInterval = Configuration.getSnapshotProfilerExportInterval(properties);
    PeriodicallyExportingStagingArea stagingArea = new PeriodicallyExportingStagingArea(StackTraceExporterProvider.INSTANCE, stagingAreaEmptyInterval);
    StagingAreaProvider.INSTANCE.configure(stagingArea);
    registerForShutdown(stagingArea);
  }

  private void activateStackTraceExporting(Resource resource, ConfigProperties properties) {
    int maxDepth = Configuration.getSnapshotProfilerStackDepth(properties);
    Duration samplingPeriod = Configuration.getSnapshotProfilerSamplingInterval(properties);
    Logger logger = buildLogger(resource, properties);
    StackTraceExporter exporter = new AsyncStackTraceExporter(logger, samplingPeriod, maxDepth);
    StackTraceExporterProvider.INSTANCE.configure(exporter);
  }

  private void registerForShutdown(Closeable closeable) {
    CloserProvider.INSTANCE.get().add(closeable);
  }

  private boolean snapshotProfilingEnabled(ConfigProperties properties) {
    return Configuration.isSnapshotProfilingEnabled(properties);
  }

  private Logger buildLogger(Resource resource, ConfigProperties properties) {
    return otelLoggerFactory.build(properties, resource);
  }
}
