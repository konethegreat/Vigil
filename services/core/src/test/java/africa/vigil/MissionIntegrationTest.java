// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — PostgreSQL-backed mission
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// integrity checks
package africa.vigil;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MissionIntegrationTest {
  @Test
  void readsAndExportsRecordTheAuthorisedPurpose() {
    var mission = create();
    String id = (String) mission.get("id");
    repository.get(OPS, id);
    repository.export(REVIEW, id);
    var accesses =
        repository.scoped(
            OPS,
            () ->
                db.queryForList(
                    "select actor,purpose,operation from mission_access where resource=?", id));
    assertTrue(
        accesses.stream()
            .anyMatch(
                row ->
                    row.get("actor").equals("reviewer")
                        && row.get("operation").equals("EXPORT")
                        && row.get("purpose").equals("MISSION_REVIEW")));
    assertEquals(0, db.queryForObject("select count(*) from mission_access", Integer.class));
  }

  @Test
  void clientCannotSupplyATenantOrRewriteMissionFields() {
    var mission = create();
    code("VALIDATION_FAILED", () -> command(mission, "generate_plan", "tenant_id", "care-demo"));
    code("VALIDATION_FAILED", () -> command(mission, "record_outcome", "status", "COMPLETED"));
  }

  @Autowired MissionService service;
  @Autowired MissionRepository repository;
  @Autowired JdbcTemplate db;
  static final Policy.Context OPS = Policy.identity("operations");
  static final Policy.Context CARE = Policy.identity("protection");
  static final Policy.Context REVIEW = Policy.identity("reviewer");

  private Map<String, Object> create() {
    return service.create(
        OPS,
        UUID.randomUUID().toString(),
        Json.object(
            "domain",
            "LIFELINES",
            "title",
            "Synthetic integration mission",
            "objective",
            "Restore and verify a fictional service.",
            "location",
            "Synthetic test area",
            "priority",
            "HIGH"));
  }

  private Map<String, Object> command(Map<String, Object> mission, String type, Object... fields) {
    var body = Json.object(fields);
    body.put("type", type);
    body.put("expected_version", mission.get("version"));
    return service.apply(OPS, (String) mission.get("id"), UUID.randomUUID().toString(), body);
  }

  private Map<String, Object> sourced() {
    return command(
        create(),
        "add_evidence",
        "title",
        "Synthetic source",
        "source",
        "Test custodian",
        "kind",
        "FIELD_REPORT",
        "statement",
        "The fictional service requires inspection.",
        "original",
        "Preserved synthetic source text.");
  }

  private static void code(String expected, Runnable work) {
    assertEquals(expected, assertThrows(Problem.class, work::run).code);
  }

  @Test
  void tenantIsolationIsEnforcedInQueriesAndDatabase() {
    var mission = create();
    String id = (String) mission.get("id");
    code("NOT_FOUND", () -> repository.get(CARE, id));
    assertTrue(repository.list(CARE).stream().noneMatch(m -> m.get("id").equals(id)));
    assertEquals(0, db.queryForObject("select count(*) from missions", Integer.class));
    assertEquals(id, repository.get(OPS, id).get("id"));
    assertEquals(0, db.queryForObject("select count(*) from missions", Integer.class));
  }

  @Test
  void readOnlyIdentityCannotMutateOrChangeDomains() {
    var mission = create();
    code(
        "POLICY_DENIED",
        () ->
            service.apply(
                REVIEW,
                (String) mission.get("id"),
                UUID.randomUUID().toString(),
                Json.object("type", "generate_plan", "expected_version", 1)));
    assertDoesNotThrow(() -> repository.get(REVIEW, (String) mission.get("id")));
    code(
        "POLICY_DENIED",
        () ->
            service.create(CARE, UUID.randomUUID().toString(), Json.object("domain", "LIFELINES")));
  }

  @Test
  void retriesReturnSavedResultAndRejectChangedPayload() {
    var mission = sourced();
    String key = UUID.randomUUID().toString();
    var body = Json.object("type", "generate_plan", "expected_version", mission.get("version"));
    var result = service.apply(OPS, (String) mission.get("id"), key, body);
    assertEquals(result, service.apply(OPS, (String) mission.get("id"), key, body));
    body.put("expected_version", result.get("version"));
    code("IDEMPOTENCY_CONFLICT", () -> service.apply(OPS, (String) mission.get("id"), key, body));
    assertEquals(3, repository.audit(OPS, (String) mission.get("id")).get("event_count"));
  }

  @Test
  void concurrentWritersCannotOverwriteEachOther() throws Exception {
    var mission = sourced();
    String source = (String) Json.rows(mission.get("evidence")).getFirst().get("id");
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var work =
          (java.util.concurrent.Callable<String>)
              () -> {
                try {
                  command(
                      mission,
                      "review_evidence",
                      "evidence_id",
                      source,
                      "status",
                      "SUPPORTED",
                      "statement",
                      "Confirmed fictional observation.",
                      "note",
                      "Reviewed with the synthetic custodian.");
                  return "OK";
                } catch (Problem error) {
                  return error.code;
                }
              };
      var one = pool.submit(work);
      var two = pool.submit(work);
      assertEquals(
          java.util.Set.of("OK", "VERSION_CONFLICT"), java.util.Set.of(one.get(), two.get()));
    }
  }

  @Test
  void sourceReviewPreservesOriginalAndInvalidatesPlan() {
    var planned = command(sourced(), "generate_plan");
    var original = Json.rows(planned.get("evidence")).getFirst();
    var reviewed =
        command(
            planned,
            "review_evidence",
            "evidence_id",
            original.get("id"),
            "status",
            "DISPUTED",
            "statement",
            "The fictional report now has a conflicting timestamp.",
            "note",
            "A second synthetic report conflicts.");
    var after = Json.rows(reviewed.get("evidence")).getFirst();
    assertEquals(original.get("original"), after.get("original"));
    assertEquals(original.get("sha256"), after.get("sha256"));
    assertEquals(1, Json.rows(after.get("history")).size());
    assertFalse(MissionRules.currentPlan(reviewed));
    code(
        "PLAN_STALE",
        () ->
            command(
                reviewed,
                "transition_task",
                "task_id",
                Json.rows(reviewed.get("tasks")).getFirst().get("id"),
                "state",
                "AUTHORISED",
                "receipt",
                "Synthetic approval."));
  }

  @Test
  void cannotSkipAcceptanceOrCloseBeforeDelivery() {
    var mission = command(sourced(), "generate_plan");
    code(
        "TASKS_INCOMPLETE",
        () ->
            command(
                mission,
                "record_outcome",
                "summary",
                "Everything looks complete.",
                "verification",
                "No actual verification yet."));
    code(
        "INVALID_TRANSITION",
        () ->
            command(
                mission,
                "transition_task",
                "task_id",
                Json.rows(mission.get("tasks")).getFirst().get("id"),
                "state",
                "CONFIRMED",
                "receipt",
                "Trying to skip the required stages."));
    assertEquals(3, repository.audit(OPS, (String) mission.get("id")).get("event_count"));
  }

  @Test
  void completeLifecycleProducesPreservedVerifiableOutcome() {
    var mission = command(sourced(), "generate_plan");
    for (var task : Json.rows(mission.get("tasks"))) {
      for (String state :
          java.util.List.of("AUTHORISED", "SENT", "ACCEPTED", "IN_PROGRESS", "CONFIRMED")) {
        mission =
            command(
                mission,
                "transition_task",
                "task_id",
                task.get("id"),
                "state",
                state,
                "receipt",
                "Synthetic receipt for " + state + " recorded by the assigned test team.");
      }
    }
    mission =
        command(
            mission,
            "record_outcome",
            "summary",
            "The fictional service is restored.",
            "verification",
            "The test custodian verified the synthetic completion record.");
    assertEquals("COMPLETED", mission.get("status"));
    var audit = repository.audit(OPS, (String) mission.get("id"));
    assertEquals(true, audit.get("verified"));
    assertEquals(19, audit.get("event_count"));
    var closed = mission;
    code(
        "MISSION_COMPLETED",
        () ->
            command(
                closed,
                "review_evidence",
                "evidence_id",
                Json.rows(closed.get("evidence")).getFirst().get("id")));
  }

  @Test
  void failedTransactionCannotLeavePartialDataOrAudit() {
    var mission = create();
    String id = (String) mission.get("id");
    int version = Json.number(mission, "version");
    assertThrows(
        IllegalStateException.class,
        () ->
            repository.command(
                OPS,
                id,
                UUID.randomUUID().toString(),
                Json.object("test", "rollback"),
                () -> {
                  var changed = repository.read(id, true);
                  changed.put("version", 99);
                  repository.save(changed);
                  repository.append(OPS, id, "TEST", "Must roll back");
                  throw new IllegalStateException("simulated interruption");
                }));
    assertEquals(version, repository.get(OPS, id).get("version"));
    assertEquals(1, repository.audit(OPS, id).get("event_count"));
  }

  @Test
  void auditCannotBeRewrittenByApplicationRole() {
    var mission = create();
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () ->
            repository.scoped(
                OPS,
                () ->
                    db.update(
                        "update mission_audit set detail='changed' where mission_id=?",
                        mission.get("id"))));
    assertEquals(true, repository.audit(OPS, (String) mission.get("id")).get("verified"));
  }
}
