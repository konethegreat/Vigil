// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job telemetry
package africa.vigil.templates.job;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Optional;

/**
 * Builds the job's tracing provider.
 *
 * <p>No exporter is installed unless a collector endpoint is configured, so the default is no
 * egress (BACKEND_GUIDE B6). Only the service name is recorded; payload content never is (B5).
 */
public final class Telemetry {

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
                                .put("service.name", "vigil-job-template")
                                .build())));
        exporter(settings).ifPresent(spanExporter ->
                builder.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build()));
        return builder.build();
    }
}
