// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job telemetry tests
package africa.vigil.templates.job;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;

class JobTelemetryTest {

    private static Settings settings(String endpoint) {
        return new Settings(
                "tenant-a", "batch-summary", "http://warden.invalid:8080", "kms://vigil/test/job#1", 10, endpoint);
    }

    @Test
    void installsNoExporterWithoutAnEndpoint() {
        assertThat(Telemetry.exporter(settings(null))).isEmpty();
    }

    @Test
    void installsTheExporterForTheConfiguredCollector() {
        assertThat(Telemetry.exporter(settings("http://collector.invalid:4318/v1/traces")))
                .isPresent();
    }

    @Test
    void buildsAProviderWithNoEndpoint() {
        try (SdkTracerProvider provider = Telemetry.provider(settings(null))) {
            provider.get("test").spanBuilder("vigil.test").startSpan().end();
        }
    }
}
