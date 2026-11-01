// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Configuration binding and startup-failure tests
package africa.vigil.templates.service;

import static org.assertj.core.api.Assertions.assertThat;

import africa.vigil.templates.service.config.Settings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Proves the production wiring, not only the annotations: a bad token reference must stop the
 * context instead of serving with a policy boundary it cannot honour.
 */
class SettingsStartupTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(Binding.class);

    @Test
    void startsWithAValidConfiguration() {
        runner.withPropertyValues(
                        "vigil.warden-url=http://warden.invalid:8080",
                        "vigil.internal-token-ref=kms://vigil/test/service-auth#1")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(Settings.class));
    }

    @Test
    void refusesToStartWithASecretInsteadOfAReference() {
        runner.withPropertyValues(
                        "vigil.warden-url=http://warden.invalid:8080",
                        "vigil.internal-token-ref=a-real-secret")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void refusesToStartWithoutAWardenUrl() {
        runner.withPropertyValues("vigil.internal-token-ref=kms://vigil/test/service-auth#1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(Settings.class)
    static class Binding {}
}
