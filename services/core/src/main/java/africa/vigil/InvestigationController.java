// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation API boundary
package africa.vigil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/investigations")
final class InvestigationController {
  private final DemoSessions sessions;
  private final InvestigationService investigations;

  InvestigationController(DemoSessions sessions, InvestigationService investigations) {
    this.sessions = sessions;
    this.investigations = investigations;
  }

  @GetMapping
  Map<String, Object> graph(HttpServletRequest request) {
    return investigations.graph(sessions.authenticate(request));
  }

  @PostMapping("/commands")
  Map<String, Object> command(
      HttpServletRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    var context = sessions.authenticate(request);
    Problem.require(
        Json.encode(body).length() <= 48000, 413, "PAYLOAD_TOO_LARGE", "The command is too large.");
    return investigations.command(context, key, body);
  }
}
