// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Validated service configuration
package africa.vigil.templates.service.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime configuration, validated at startup.
 *
 * <p>Secrets are KMS references, never values (INSTRUCTIONS §9.10). An invalid configuration stops
 * the process rather than serving with a half-known policy boundary.
 */
@Validated
@ConfigurationProperties(prefix = "vigil")
public record Settings(
        @DefaultValue("vigil-service-template") String serviceName,
        @DefaultValue("dev") String environment,
        @NotBlank @Pattern(regexp = "^https?://.+") String wardenUrl,
        @NotBlank @Pattern(regexp = "^kms://.+") String internalTokenRef,
        @Pattern(regexp = "^https?://.+") String otelEndpoint) {

    public Settings {
        if (otelEndpoint != null && otelEndpoint.isBlank()) {
            otelEndpoint = null;
        }
    }
}
