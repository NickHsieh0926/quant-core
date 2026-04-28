package com.hcy.quant_core.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

  @Bean
  public OpenTelemetry openTelemetry(@Value("${management.otlp.tracing.endpoint}") String endpoint,
      @Value("${spring.application.name}") String appName) {

    return OpenTelemetrySdk.builder().setTracerProvider(SdkTracerProvider.builder().setResource(
                Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), appName)))
            .addSpanProcessor(
                BatchSpanProcessor.builder(OtlpHttpSpanExporter.builder().setEndpoint(endpoint).build())
                    .build()).build())
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();
  }

  @Bean
  public OtelCurrentTraceContext otelCurrentTraceContext() {
    return new OtelCurrentTraceContext();
  }

  @Bean
  public io.micrometer.tracing.Tracer tracer(OpenTelemetry openTelemetry,
      OtelCurrentTraceContext otelCurrentTraceContext) {
    return new OtelTracer(openTelemetry.getTracer("quant-core"), otelCurrentTraceContext, event -> {
    });
  }

  @Bean
  public ObservationRegistryCustomizer<ObservationRegistry> tracingObservationRegistryCustomizer(
      io.micrometer.tracing.Tracer tracer) {
    return registry -> registry.observationConfig()
        .observationHandler(new DefaultTracingObservationHandler(tracer));
  }

}
