// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job configuration tests
package africa.vigil.templates.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsTest {

    private static Map<String, String> environment() {
        Map<String, String> env = new HashMap<>();
        env.put("VIGIL_TENANT_ID", "tenant-a");
        env.put("VIGIL_PURPOSE", "batch-summary");
        env.put("VIGIL_WARDEN_URL", "http://warden.invalid:8080");
        env.put("VIGIL_POLICY_TOKEN_REF", "kms://vigil/test/job#1");
        return env;
    }

    @Test
    void readsAValidEnvironment() {
        Settings settings = Settings.fromEnvironment(environment());

        assertThat(settings.tenantId()).isEqualTo("tenant-a");
        assertThat(settings.purpose()).isEqualTo("batch-summary");
        assertThat(settings.batchSize()).isEqualTo(100);
        assertThat(settings.otelEndpoint()).isNull();
    }

    @Test
    void namesTheMissingTenantWithoutPrintingValues() {
        Map<String, String> env = environment();
        env.remove("VIGIL_TENANT_ID");

        assertThatThrownBy(() -> Settings.fromEnvironment(env))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("VIGIL_TENANT_ID");
    }

    @Test
    void namesTheMissingPurposeWithoutPrintingValues() {
        Map<String, String> env = environment();
        env.remove("VIGIL_PURPOSE");

        assertThatThrownBy(() -> Settings.fromEnvironment(env))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("VIGIL_PURPOSE");
    }

    @Test
    void rejectsANonHttpWardenUrl() {
        Map<String, String> env = environment();
        env.put("VIGIL_WARDEN_URL", "warden:8080");

        assertThatThrownBy(() -> Settings.fromEnvironment(env))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("VIGIL_WARDEN_URL");
    }

    @Test
    void rejectsATokenThatIsNotAReferenceWithoutPrintingIt() {
        Map<String, String> env = environment();
        env.put("VIGIL_POLICY_TOKEN_REF", "a-real-secret");

        assertThatThrownBy(() -> Settings.fromEnvironment(env))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("VIGIL_POLICY_TOKEN_REF")
                .hasMessageNotContaining("a-real-secret");
    }

    @Test
    void rejectsANonPositiveBatchSize() {
        Map<String, String> env = environment();
        env.put("VIGIL_BATCH_SIZE", "0");

        assertThatThrownBy(() -> Settings.fromEnvironment(env))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("VIGIL_BATCH_SIZE");
    }

    @Test
    void rejectsANonNumericBatchSize() {
        Map<String, String> env = environment();
        env.put("VIGIL_BATCH_SIZE", "many");

        assertThatThrownBy(() -> Settings.fromEnvironment(env))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("VIGIL_BATCH_SIZE");
    }

    @Test
    void treatsABlankEndpointAsOff() {
        Map<String, String> env = environment();
        env.put("VIGIL_OTEL_ENDPOINT", " ");

        assertThat(Settings.fromEnvironment(env).otelEndpoint()).isNull();
    }

    @Test
    void rejectsANonHttpEndpoint() {
        Map<String, String> env = environment();
        env.put("VIGIL_OTEL_ENDPOINT", "collector:4318");

        assertThatThrownBy(() -> Settings.fromEnvironment(env))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("VIGIL_OTEL_ENDPOINT");
    }
}
