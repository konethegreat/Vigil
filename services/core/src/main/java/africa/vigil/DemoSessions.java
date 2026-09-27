// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Signed, expiring synthetic
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// identities
package africa.vigil;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class DemoSessions {
  private final String internalToken;
  private final byte[] sessionKey;

  DemoSessions(
      @Value("${VIGIL_DEMO_MODE:false}") boolean demoMode,
      @Value("${VIGIL_INTERNAL_TOKEN:}") String internalToken,
      @Value("${VIGIL_SESSION_KEY:}") String sessionKey) {
    if (!demoMode || internalToken.length() < 16 || sessionKey.length() < 32)
      throw new IllegalStateException(
          "Explicit synthetic demo mode and configured service/session keys are required.");
    this.internalToken = internalToken;
    this.sessionKey = sessionKey.getBytes(StandardCharsets.UTF_8);
  }

  void requireGateway(HttpServletRequest request) {
    String supplied = request.getHeader("X-Vigil-Gateway");
    Problem.require(
        supplied != null
            && MessageDigest.isEqual(
                internalToken.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8)),
        401,
        "GATEWAY_REQUIRED",
        "Use the Vigil workspace to access the service.");
  }

  String issue(String actor) {
    Policy.identity(actor);
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                (actor + "|" + Instant.now().plusSeconds(3600).getEpochSecond())
                    .getBytes(StandardCharsets.UTF_8));
    return payload + "." + signature(payload);
  }

  Policy.Context authenticate(HttpServletRequest request) {
    requireGateway(request);
    String token = request.getHeader("Authorization");
    Problem.require(
        token != null && token.startsWith("Bearer "),
        401,
        "INVALID_SESSION",
        "Your workspace session has expired.");
    try {
      String[] parts = token.substring(7).split("\\.", -1);
      Problem.require(
          parts.length == 2
              && MessageDigest.isEqual(
                  signature(parts[0]).getBytes(StandardCharsets.UTF_8),
                  parts[1].getBytes(StandardCharsets.UTF_8)),
          401,
          "INVALID_SESSION",
          "Invalid workspace session.");
      String[] content =
          new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split("\\|");
      Problem.require(
          content.length == 2 && Long.parseLong(content[1]) > Instant.now().getEpochSecond(),
          401,
          "INVALID_SESSION",
          "Your workspace session has expired.");
      return Policy.identity(content[0]);
    } catch (Problem e) {
      throw e;
    } catch (RuntimeException e) {
      throw new Problem(401, "INVALID_SESSION", "Invalid workspace session.");
    }
  }

  private String signature(String payload) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(sessionKey, "HmacSHA256"));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Session signing unavailable", e);
    }
  }
}
