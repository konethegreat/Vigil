// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Opt-in OTLP tracing
package africa.vigil.templates.service.telemetry;

import africa.vigil.templates.service.config.Settings;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Optional;

/**
 * Builds the tracing provider.
 *
 * <p>No exporter is installed unless an in-boundary collector endpoint is configured, so the
 * default is no egress (BACKEND_GUIDE B6, INSTRUCTIONS §9.8). Only the service name and
 * environment are recorded as resource attributes; request content never is (B5).
 */
public final class Telemetry {

    private static final String SERVICE_NAME = "service.name";
    private static final String DEPLOYMENT_ENVIRONMENT = "deployment.environment.name";

    private Telemetry() {}

    /** The OTLP exporter, or empty when no collector is configured (no export, no egress). */
    public static Optional<SpanExporter> exporter(Settings settings) {
        if (settings.otelEndpoint() == null) {
            return Optional.empty();
        }
        return Optional.of(OtlpHttpSpanExporter.builder()
                .setEndpoint(settings.otelEndpoint())
                .build());
    }

    public static SdkTracerProvider provider(Settings settings) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setResource(Resource.getDefault()
                        .merge(Resource.create(Attributes.builder()
                                .put(SERVICE_NAME, settings.serviceName())
                                .put(DEPLOYMENT_ENVIRONMENT, settings.environment())
                                .build())));
        exporter(settings).ifPresent(spanExporter ->
                builder.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build()));
        return builder.build();
    }
}
