// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Health probe tests
package africa.vigil.templates.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired private MockMvc mvc;

    @Test
    void livenessAlwaysAnswers() throws Exception {
        mvc.perform(get("/healthz/live")).andExpect(status().isOk());
    }

    @Test
    void startupAnswersOnceTheContextServes() throws Exception {
        mvc.perform(get("/healthz/startup")).andExpect(status().isOk());
    }

    @Test
    void readinessFailsClosedWhileTheStubIsInPlace() throws Exception {
        mvc.perform(get("/healthz/ready")).andExpect(status().isServiceUnavailable());
    }
}
