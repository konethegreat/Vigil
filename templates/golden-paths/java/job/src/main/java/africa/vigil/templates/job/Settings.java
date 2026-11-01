// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job configuration
package africa.vigil.templates.job;

import java.util.Map;

/**
 * One-shot job configuration, read once from the environment and validated before any work.
 *
 * <p>Errors name the variable and never print its value (BACKEND_GUIDE B5). Secrets stay as KMS
 * references (INSTRUCTIONS §9.10).
 */
public record Settings(
        String tenantId,
        String purpose,
        String wardenUrl,
        String policyTokenRef,
        int batchSize,
        String otelEndpoint) {

    static final int DEFAULT_BATCH_SIZE = 100;

    public static Settings fromEnvironment(Map<String, String> environment) {
        String tenantId = required(environment, "VIGIL_TENANT_ID");
        String purpose = required(environment, "VIGIL_PURPOSE");

        String wardenUrl = required(environment, "VIGIL_WARDEN_URL");
        if (!isHttp(wardenUrl)) {
            throw new ConfigurationException("VIGIL_WARDEN_URL must start with http:// or https://");
        }

        String tokenRef = required(environment, "VIGIL_POLICY_TOKEN_REF");
        if (!tokenRef.startsWith("kms://")) {
            throw new ConfigurationException("VIGIL_POLICY_TOKEN_REF must be a kms:// reference");
        }

        return new Settings(
                tenantId,
                purpose,
                wardenUrl,
                tokenRef,
                batchSize(environment.get("VIGIL_BATCH_SIZE")),
                endpoint(environment.get("VIGIL_OTEL_ENDPOINT")));
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(name + " is required");
        }
        return value;
    }

    private static int batchSize(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BATCH_SIZE;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException("VIGIL_BATCH_SIZE must be a positive integer");
        }
        if (parsed < 1) {
            throw new ConfigurationException("VIGIL_BATCH_SIZE must be a positive integer");
        }
        return parsed;
    }

    private static String endpoint(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!isHttp(value)) {
            throw new ConfigurationException("VIGIL_OTEL_ENDPOINT must start with http:// or https://");
        }
        return value;
    }

    private static boolean isHttp(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
