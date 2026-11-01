// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy seam tests with the stub
package africa.vigil.templates.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import africa.vigil.templates.service.policy.PolicyUnavailableException;
import africa.vigil.templates.service.policy.UnavailableWarden;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PolicySeamTest {

    private static final String TOKEN = "synthetic-policy-token";

    @Autowired private MockMvc mvc;

    @Test
    void theStubFailsClosedWith503() throws Exception {
        mvc.perform(get("/v1/example")
                        .header("X-Vigil-Tenant", "tenant-a")
                        .header("X-Vigil-Purpose", "investigation")
                        .header("X-Vigil-Policy-Token", TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(Matchers.not(Matchers.containsString(TOKEN))));
    }

    @Test
    void aMissingHeaderIs400WithoutEchoingTheToken() throws Exception {
        mvc.perform(get("/v1/example")
                        .header("X-Vigil-Purpose", "investigation")
                        .header("X-Vigil-Policy-Token", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.not(Matchers.containsString(TOKEN))));
    }

    @Test
    void theStubReportsItselfUnready() {
        assertThatThrownBy(() -> new UnavailableWarden().authorize("tenant-a", "investigation", TOKEN))
                .isInstanceOf(PolicyUnavailableException.class);
    }
}
