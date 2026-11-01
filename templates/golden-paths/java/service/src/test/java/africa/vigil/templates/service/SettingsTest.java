// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Service settings validation tests
package africa.vigil.templates.service;

import static org.assertj.core.api.Assertions.assertThat;

import africa.vigil.templates.service.config.Settings;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class SettingsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static Settings settings(String wardenUrl, String tokenRef, String otelEndpoint) {
        return new Settings("vigil-service-template", "dev", wardenUrl, tokenRef, otelEndpoint);
    }

    @Test
    void acceptsAValidConfiguration() {
        Settings settings = settings("http://warden:8080", "kms://vigil/dev/service#1", null);

        assertThat(validator.validate(settings)).isEmpty();
    }

    @Test
    void rejectsANonHttpWardenUrl() {
        Settings settings = settings("warden:8080", "kms://vigil/dev/service#1", null);

        assertThat(validator.validate(settings))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                        .isEqualTo("wardenUrl"));
    }

    @Test
    void rejectsATokenThatIsNotAReference() {
        Settings settings = settings("http://warden:8080", "a-real-secret", null);

        assertThat(validator.validate(settings))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                        .isEqualTo("internalTokenRef"));
    }

    @Test
    void rejectsABlankWardenUrl() {
        Settings settings = settings(" ", "kms://vigil/dev/service#1", null);

        assertThat(validator.validate(settings)).isNotEmpty();
    }

    @Test
    void rejectsANonHttpTelemetryEndpoint() {
        Settings settings = settings("http://warden:8080", "kms://vigil/dev/service#1", "collector:4318");

        assertThat(validator.validate(settings))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                        .isEqualTo("otelEndpoint"));
    }

    @Test
    void normalisesABlankEndpointToOff() {
        assertThat(settings("http://warden:8080", "kms://vigil/dev/service#1", " ").otelEndpoint())
                .isNull();
    }
}
