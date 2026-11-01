// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Web wiring
package africa.vigil.templates.service.web;

import africa.vigil.templates.service.policy.WardenBinding;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the policy resolver so every data-bearing route receives a verified context. */
@Configuration(proxyBeanMethods = false)
class WebConfiguration implements WebMvcConfigurer {

    private final WardenBinding warden;

    WebConfiguration(WardenBinding warden) {
        this.warden = warden;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PolicyContextArgumentResolver(warden));
    }
}
