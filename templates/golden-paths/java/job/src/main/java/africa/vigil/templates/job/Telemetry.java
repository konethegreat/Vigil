// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job telemetry
package africa.vigil.templates.job;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

/**
 * Builds the job's tracing provider.
 *
 * <p>No exporter is installed unless a collector endpoint is configured, so the default is no
 * egress (BACKEND_GUIDE B6). Only the service name is recorded; payload content never is (B5).
 */
public final class Telemetry {

    private Telemetry() {}

    /** Starts an exporter only when {@code settings.otelEndpoint()} is present. */
    public static SdkTracerProvider provider(Settings settings) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setResource(Resource.getDefault()
                        .merge(Resource.create(Attributes.builder()
                                .put("service.name", "vigil-job-template")
                                .build())));
        if (settings.otelEndpoint() != null) {
            builder.addSpanProcessor(BatchSpanProcessor.builder(OtlpHttpSpanExporter.builder()
                            .setEndpoint(settings.otelEndpoint())
                            .build())
                    .build());
        }
        return builder.build();
    }
}
