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

package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class OpenTelemetrySdkExtension extends AutoConfiguredOpenTelemetrySdk
    implements OpenTelemetry,
        BeforeEachCallback,
        AfterEachCallback,
        ParameterResolver,
        AutoCloseable {
  public static Builder configure() {
    return new Builder();
  }

  private final OpenTelemetrySdk sdk;
  private final ConfigProperties properties;
  private final List<AgentListener> agentListeners;

  private OpenTelemetrySdkExtension(
      OpenTelemetrySdk sdk, ConfigProperties properties, List<AgentListener> agentListeners) {
    this.sdk = sdk;
    this.properties = properties;
    this.agentListeners = agentListeners;
  }

  @Override
  public TracerProvider getTracerProvider() {
    return sdk.getTracerProvider();
  }

  @Override
  public MeterProvider getMeterProvider() {
    return sdk.getMeterProvider();
  }

  @Override
  public LoggerProvider getLogsBridge() {
    return sdk.getLogsBridge();
  }

  @Override
  public ContextPropagators getPropagators() {
    return sdk.getPropagators();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    agentListeners.forEach(listener -> listener.afterAgent(this));
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    sdk.close();
  }

  @Override
  public void close() {
    sdk.close();
  }

  @Override
  public OpenTelemetrySdk getOpenTelemetrySdk() {
    return sdk;
  }

  @Override
  Resource getResource() {
    return Resource.getDefault();
  }

  @Override
  public ConfigProperties getConfig() {
    return properties;
  }

  @Override
  Object getConfigProvider() {
    return null;
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == Tracer.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return sdk.getTracer(extensionContext.getRequiredTestClass().getName(), "test");
  }

  /**
   * An extremely simplified adaptation of the OpenTelemetry class
   * AutoConfiguredOpenTelemetrySdkBuilder, designed explicitly to facilitate easier component-like
   * testing of custom OpenTelemetry Java Agent extensions.
   */
  public static class Builder {
    private final SdkCustomizer customizer = new SdkCustomizer();
    private final Map<String, String> propertyOverrides = new HashMap<>();
    private final List<TextMapPropagator> propagators = new ArrayList<>();
    private final List<AgentListener> agentListeners = new ArrayList<>();
    private Sampler sampler = Sampler.alwaysOn();

    public Builder withProperty(String name, String value) {
      propertyOverrides.put(name, value);
      return this;
    }

    public Builder with(AutoConfigurationCustomizerProvider provider) {
      provider.customize(customizer);
      return this;
    }

    public Builder withSampler(Sampler sampler) {
      this.sampler = sampler;
      return this;
    }

    public Builder with(TextMapPropagator propagator) {
      this.propagators.add(propagator);
      return this;
    }

    public Builder with(AgentListener agentListener) {
      this.agentListeners.add(agentListener);
      return this;
    }

    /**
     * Simplified re-implementation of AutoConfiguredOpenTelemetrySdkBuilder's build method. The
     * OpenTelemetry SDK is only configured with features necessary to pass existing test use cases.
     */
    public OpenTelemetrySdkExtension build() {
      ConfigProperties configProperties = customizeProperties();
      SdkTracerProvider tracerProvider = customizeTracerProvider(configProperties);

      TextMapPropagator propagator = TextMapPropagator.composite(configuredPropagators());
      ContextPropagators contextPropagators = ContextPropagators.create(propagator);

      OpenTelemetrySdk sdk =
          OpenTelemetrySdk.builder()
              .setTracerProvider(tracerProvider)
              .setPropagators(contextPropagators)
              .build();
      return new OpenTelemetrySdkExtension(sdk, configProperties, agentListeners);
    }

    private ConfigProperties customizeProperties() {
      var properties =
          DefaultConfigProperties.createFromMap(customizer.defaultProperties)
              .withOverrides(propertyOverrides);
      for (var customizer : customizer.propertyCustomizers) {
        var overrides = customizer.apply(properties);
        properties = properties.withOverrides(overrides);
      }
      return properties;
    }

    private SdkTracerProvider customizeTracerProvider(ConfigProperties properties) {
      var builder = SdkTracerProvider.builder().setSampler(sampler);
      customizer.tracerProviderCustomizers.forEach(
          customizer -> customizer.apply(builder, properties));
      return builder.build();
    }

    private List<TextMapPropagator> configuredPropagators() {
      List<TextMapPropagator> propagators = new ArrayList<>();
      propagators.add(W3CBaggagePropagator.getInstance());
      propagators.add(W3CTraceContextPropagator.getInstance());
      propagators.addAll(this.propagators);
      return propagators;
    }
  }

  private static class SdkCustomizer implements AutoConfigurationCustomizer {
    private final Map<String, String> defaultProperties = new HashMap<>();
    private final List<Function<ConfigProperties, Map<String, String>>> propertyCustomizers =
        new ArrayList<>();
    private final List<
            BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>>
        tracerProviderCustomizers = new ArrayList<>();

    @Override
    public AutoConfigurationCustomizer addTracerProviderCustomizer(
        BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
            tracerProviderCustomizer) {
      tracerProviderCustomizers.add(Objects.requireNonNull(tracerProviderCustomizer));
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropagatorCustomizer(
        BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator>
            textMapPropagator) {
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesCustomizer(
        Function<ConfigProperties, Map<String, String>> propertiesCustomizer) {
      this.propertyCustomizers.add(propertiesCustomizer);
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addResourceCustomizer(
        BiFunction<? super Resource, ConfigProperties, ? extends Resource> biFunction) {
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addSamplerCustomizer(
        BiFunction<? super Sampler, ConfigProperties, ? extends Sampler> biFunction) {
      return null;
    }

    @Override
    public AutoConfigurationCustomizer addSpanExporterCustomizer(
        BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter> biFunction) {
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesSupplier(
        Supplier<Map<String, String>> supplier) {
      defaultProperties.putAll(supplier.get());
      return this;
    }
  }
}
