// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Session boundary checks
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
package africa.vigil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class SessionTest {
  final DemoSessions sessions =
      new DemoSessions(
          true, "local-synthetic-mission-only", "local-synthetic-session-key-not-for-production");

  @Test
  void requiresExplicitDemoConfiguration() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new DemoSessions(
                false,
                "local-synthetic-mission-only",
                "local-synthetic-session-key-not-for-production"));
  }

  @Test
  void rejectsMissingGatewayAndForgedIdentity() {
    var request = new MockHttpServletRequest();
    assertEquals(
        "GATEWAY_REQUIRED", assertThrows(Problem.class, () -> sessions.authenticate(request)).code);
    request.addHeader("X-Vigil-Gateway", "local-synthetic-mission-only");
    request.addHeader("Authorization", "Bearer forged.signature");
    assertEquals(
        "INVALID_SESSION", assertThrows(Problem.class, () -> sessions.authenticate(request)).code);
  }

  @Test
  void signedSessionSuppliesTenantAndPurpose() {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Vigil-Gateway", "local-synthetic-mission-only");
    request.addHeader("Authorization", "Bearer " + sessions.issue("protection"));
    assertEquals(Policy.identity("protection"), sessions.authenticate(request));
    request.addHeader("X-Tenant-Id", "operations-demo");
    assertEquals("care-demo", sessions.authenticate(request).tenant());
  }
}
