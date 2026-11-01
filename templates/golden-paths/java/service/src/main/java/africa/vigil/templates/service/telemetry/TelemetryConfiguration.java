// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Tracing beans
package africa.vigil.templates.service.telemetry;

import africa.vigil.templates.service.config.Settings;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Provides one SDK provider per process and the tracer the routes use. */
@Configuration(proxyBeanMethods = false)
class TelemetryConfiguration {

    @Bean(destroyMethod = "close")
    SdkTracerProvider tracerProvider(Settings settings) {
        return Telemetry.provider(settings);
    }

    @Bean
    Tracer tracer(SdkTracerProvider provider) {
        return provider.get("africa.vigil.templates.service");
    }
}
