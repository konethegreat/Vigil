// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Authenticated mission API
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
package africa.vigil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
final class MissionController {
  private final DemoSessions sessions;
  private final MissionRepository repository;
  private final MissionService missions;
  private final MindClient mind;
  private final JdbcTemplate db;

  MissionController(
      DemoSessions sessions,
      MissionRepository repository,
      MissionService missions,
      MindClient mind,
      JdbcTemplate db) {
    this.sessions = sessions;
    this.repository = repository;
    this.missions = missions;
    this.mind = mind;
    this.db = db;
  }

  @GetMapping("/health")
  Map<String, Object> health() {
    db.queryForObject("select 1", Integer.class);
    return Json.object("status", "ok", "service", "vigil-core", "mode", "synthetic-demo");
  }

  @PostMapping("/session")
  Map<String, Object> session(HttpServletRequest request, @RequestBody Map<String, Object> body) {
    sessions.requireGateway(request);
    String actor = MissionRules.text(body, "actor", 3, 30);
    return Json.object(
        "token", sessions.issue(actor), "identity", Policy.identity(actor).publicView());
  }

  @GetMapping("/session")
  Map<String, Object> identity(HttpServletRequest request) {
    return sessions.authenticate(request).publicView();
  }

  @GetMapping("/missions")
  Map<String, Object> list(HttpServletRequest request) {
    var context = sessions.authenticate(request);
    return Json.object("identity", context.publicView(), "missions", repository.list(context));
  }

  @GetMapping("/missions/{id}")
  Map<String, Object> detail(HttpServletRequest request, @PathVariable String id) {
    return repository.get(sessions.authenticate(request), id);
  }

  @PostMapping("/missions")
  Map<String, Object> create(
      HttpServletRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    return missions.create(sessions.authenticate(request), key, body);
  }

  @PostMapping("/missions/{id}/commands")
  Map<String, Object> command(
      HttpServletRequest request,
      @PathVariable String id,
      @RequestHeader(value = "Idempotency-Key", required = false) String key,
      @RequestBody Map<String, Object> body) {
    Problem.require(
        Json.encode(body).length() <= 48000, 413, "PAYLOAD_TOO_LARGE", "The command is too large.");
    return missions.apply(sessions.authenticate(request), id, key, body);
  }

  @GetMapping("/missions/{id}/audit")
  Map<String, Object> audit(HttpServletRequest request, @PathVariable String id) {
    return repository.audit(sessions.authenticate(request), id);
  }

  @GetMapping("/intelligence/status")
  Map<String, Object> intelligence(HttpServletRequest request) {
    sessions.authenticate(request);
    return mind.status();
  }

  @GetMapping("/missions/{id}/export")
  Map<String, Object> export(HttpServletRequest request, @PathVariable String id) {
    var context = sessions.authenticate(request);
    return repository.export(context, id);
  }
}

@RestControllerAdvice
final class ApiErrors {
  @ExceptionHandler(Problem.class)
  ResponseEntity<?> problem(Problem error) {
    return ResponseEntity.status(error.status)
        .body(
            Json.object(
                "error",
                Json.object(
                    "code",
                    error.code,
                    "message",
                    error.getMessage(),
                    "request_id",
                    UUID.randomUUID().toString())));
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  ResponseEntity<?> invalidBody(Exception error) {
    return ResponseEntity.badRequest()
        .body(
            Json.object(
                "error",
                Json.object(
                    "code", "VALIDATION_FAILED", "message", "Provide a valid JSON request.")));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception error) {
    return ResponseEntity.internalServerError()
        .body(
            Json.object(
                "error",
                Json.object(
                    "code",
                    "INTERNAL_ERROR",
                    "message",
                    "The operation could not be completed. No uncommitted change was applied.",
                    "request_id",
                    UUID.randomUUID().toString())));
  }
}
