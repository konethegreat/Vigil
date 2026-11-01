// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Authorized policy seam tests
package africa.vigil.templates.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import africa.vigil.templates.service.policy.PolicyContext;
import africa.vigil.templates.service.policy.WardenBinding;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PolicyAllowedTest.Allowing.class)
class PolicyAllowedTest {

    private static final String TOKEN = "synthetic-policy-token";

    @Autowired private MockMvc mvc;

    @Test
    void anAuthorizedRequestReachesTheRoute() throws Exception {
        mvc.perform(get("/v1/example")
                        .header("X-Vigil-Tenant", "tenant-a")
                        .header("X-Vigil-Purpose", "investigation")
                        .header("X-Vigil-Policy-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant").value("tenant-a"))
                .andExpect(jsonPath("$.purpose").value("investigation"))
                .andExpect(content().string(Matchers.not(Matchers.containsString(TOKEN))));
    }

    @Test
    void readinessTurnsGreenOnlyWhenWardenIsReady() throws Exception {
        mvc.perform(get("/healthz/ready")).andExpect(status().isOk());
    }

    @TestConfiguration
    static class Allowing {
        @Bean
        @Primary
        WardenBinding allowing() {
            return new WardenBinding() {
                @Override
                public boolean ready() {
                    return true;
                }

                @Override
                public PolicyContext authorize(String tenantId, String purpose, String token) {
                    return new PolicyContext(tenantId, purpose, token);
                }
            };
        }
    }
}
