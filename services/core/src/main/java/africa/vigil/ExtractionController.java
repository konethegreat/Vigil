// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Source review API
package africa.vigil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
final class ExtractionController {
  private final DemoSessions sessions;
  private final ExtractionService extractions;

  ExtractionController(DemoSessions sessions, ExtractionService extractions) {
    this.sessions = sessions;
    this.extractions = extractions;
  }

  @GetMapping("/missions/{id}/extractions")
  Map<String, Object> list(HttpServletRequest request, @PathVariable String id) {
    return extractions.list(sessions.authenticate(request), id);
  }

  @PostMapping("/extractions")
  Map<String, Object> propose(
      HttpServletRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return extractions.propose(sessions.authenticate(request), key, body);
  }

  @PostMapping("/extractions/{id}/review")
  Map<String, Object> review(
      HttpServletRequest request,
      @PathVariable String id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    Problem.require(
        Json.encode(body).length() <= 8000, 413, "PAYLOAD_TOO_LARGE", "The review is too large.");
    return extractions.review(sessions.authenticate(request), id, key, body);
  }
}
