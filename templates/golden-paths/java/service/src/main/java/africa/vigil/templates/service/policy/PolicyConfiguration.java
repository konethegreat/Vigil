// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Default Warden binding
package africa.vigil.templates.service.policy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the fail-closed stub. The P3 binding replaces this bean; no bypass arrives with it. */
@Configuration(proxyBeanMethods = false)
class PolicyConfiguration {

    @Bean
    WardenBinding wardenBinding() {
        return new UnavailableWarden();
    }
}
