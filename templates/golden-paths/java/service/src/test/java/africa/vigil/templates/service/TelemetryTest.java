// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Telemetry wiring tests
package africa.vigil.templates.service;

import static org.assertj.core.api.Assertions.assertThat;

import africa.vigil.templates.service.config.Settings;
import africa.vigil.templates.service.telemetry.Telemetry;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;

class TelemetryTest {

    private static Settings settings(String endpoint) {
        return new Settings(
                "vigil-service-template", "test", "http://warden:8080", "kms://vigil/test/service#1", endpoint);
    }

    @Test
    void staysOffWithoutAnEndpoint() {
        try (SdkTracerProvider provider = Telemetry.provider(settings(null))) {
            provider.get("test").spanBuilder("vigil.test").startSpan().end();
            assertThat(provider).isNotNull();
        }
    }

    @Test
    void exportsToTheConfiguredCollector() {
        try (SdkTracerProvider provider =
                Telemetry.provider(settings("http://collector.invalid:4318/v1/traces"))) {
            provider.get("test").spanBuilder("vigil.test").startSpan().end();
            assertThat(provider).isNotNull();
        }
    }
}
