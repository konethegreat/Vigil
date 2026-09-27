// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Scoped observations and agency
// API
package africa.vigil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coordination")
final class CoordinationController {
  private final DemoSessions sessions;
  private final ObservationService observations;
  private final CoordinationService coordination;

  CoordinationController(
      DemoSessions sessions, ObservationService observations, CoordinationService coordination) {
    this.sessions = sessions;
    this.observations = observations;
    this.coordination = coordination;
  }

  private Map<String, Object> bounded(Map<String, Object> body) {
    Problem.require(
        Json.encode(body).length() <= 32000,
        413,
        "PAYLOAD_TOO_LARGE",
        "Coordination request is too large.");
    return body;
  }

  @GetMapping("/workspace")
  Map<String, Object> workspace(HttpServletRequest request) {
    return coordination.workspace(sessions.authenticate(request));
  }

  @PostMapping("/sources")
  Map<String, Object> source(
      HttpServletRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return observations.register(sessions.authenticate(request), key, bounded(body));
  }

  @PostMapping("/sources/{id}/status")
  Map<String, Object> status(
      HttpServletRequest request,
      @PathVariable String id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return observations.status(sessions.authenticate(request), id, key, bounded(body));
  }

  @PostMapping("/observations")
  Map<String, Object> ingest(
      HttpServletRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return observations.ingest(sessions.authenticate(request), key, bounded(body));
  }

  @PostMapping("/search")
  Map<String, Object> search(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    return observations.search(sessions.authenticate(request), bounded(body));
  }

  @PostMapping("/cases")
  Map<String, Object> createCase(
      HttpServletRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return coordination.createCase(sessions.authenticate(request), key, bounded(body));
  }

  @GetMapping("/cases/{id}")
  Map<String, Object> details(HttpServletRequest request, @PathVariable String id) {
    return coordination.details(sessions.authenticate(request), id, false);
  }

  @GetMapping("/cases/{id}/export")
  Map<String, Object> export(HttpServletRequest request, @PathVariable String id) {
    return coordination.details(sessions.authenticate(request), id, true);
  }

  @PostMapping("/requests")
  Map<String, Object> draft(
      HttpServletRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return coordination.draft(sessions.authenticate(request), key, bounded(body));
  }

  @PostMapping("/requests/{id}/commands")
  Map<String, Object> command(
      HttpServletRequest request,
      @PathVariable String id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return coordination.command(sessions.authenticate(request), id, key, bounded(body));
  }
}
