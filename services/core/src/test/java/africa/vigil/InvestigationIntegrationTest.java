// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation integrity and
// isolation
package africa.vigil;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class InvestigationIntegrationTest {
  @Autowired InvestigationService graph;
  @Autowired MissionService service;
  @Autowired MissionRepository repository;
  @Autowired JdbcTemplate db;
  static final Policy.Context OPS = Policy.identity("operations");
  static final Policy.Context CARE = Policy.identity("protection");
  static final Policy.Context REVIEW = Policy.identity("reviewer");

  private Map<String, Object> sourced(Policy.Context context) {
    var mission =
        service.create(
            context,
            key(),
            Json.object(
                "domain",
                context == CARE ? "PROTECT" : "INVESTIGATE",
                "title",
                "Synthetic graph verification",
                "objective",
                "Verify source-backed relationships and unresolved claims.",
                "location",
                "Fictional test district",
                "priority",
                "STANDARD"));
    return service.apply(
        context,
        (String) mission.get("id"),
        key(),
        Json.object(
            "type",
            "add_evidence",
            "expected_version",
            mission.get("version"),
            "title",
            "Fictional source",
            "source",
            "Test custodian",
            "kind",
            "DOCUMENT",
            "statement",
            "An unverified fictional source assertion.",
            "original",
            "Original synthetic text retained intact."));
  }

  private static String key() {
    return UUID.randomUUID().toString();
  }

  private String source(Map<String, Object> mission) {
    return (String) Json.rows(mission.get("evidence")).getFirst().get("id");
  }

  private Map<String, Object> request(
      Policy.Context context, Map<String, Object> mission, String operation, Object... fields) {
    var request = Json.object(fields);
    request.put("type", operation);
    request.put("mission_id", mission.get("id"));
    request.put(
        "expected_version", repository.get(context, (String) mission.get("id")).get("version"));
    return request;
  }

  private Map<String, Object> objectRequest(Map<String, Object> mission, String type) {
    return request(
        OPS,
        mission,
        "create_object",
        "object_type",
        type,
        "title",
        "Fictional object",
        "summary",
        "An explicitly synthetic observed object.",
        "evidence_id",
        source(mission));
  }

  private String object(Map<String, Object> mission, String type) {
    return (String) graph.command(OPS, key(), objectRequest(mission, type)).get("record_id");
  }

  private Map<String, Object> linkRequest(Map<String, Object> mission, String from, String to) {
    return request(
        OPS,
        mission,
        "create_link",
        "from_id",
        from,
        "to_id",
        to,
        "relation",
        "RELATED_TO",
        "evidence_id",
        source(mission),
        "assessment",
        "REPORTED",
        "reason",
        "The source reports this relationship; verification is pending.");
  }

  private Map<String, Object> record(String collection, String id) {
    return Json.rows(graph.graph(OPS).get(collection)).stream()
        .filter(r -> id.equals(r.get("id")))
        .findFirst()
        .orElseThrow();
  }

  private void code(String expected, Runnable work) {
    assertEquals(expected, assertThrows(Problem.class, work::run).code);
  }

  @Test
  void sharedObjectsRetainOriginAndAssertionMissionAndVerifiedAudit() {
    var first = sourced(OPS);
    var second = sourced(OPS);
    String from = object(first, "ASSET"), to = object(first, "ORGANISATION");
    String id = (String) graph.command(OPS, key(), linkRequest(second, from, to)).get("record_id");
    var link = record("links", id);
    assertEquals(second.get("id"), link.get("mission_id"));
    assertEquals(first.get("id"), record("objects", from).get("mission_id"));
    assertEquals(source(second), link.get("evidence_id"));
    assertEquals(
        Json.rows(second.get("evidence")).getFirst().get("sha256"), link.get("original_sha256"));
    assertEquals(1, Json.rows(link.get("history")).size());
    assertEquals(true, repository.audit(OPS, (String) second.get("id")).get("verified"));
    assertFalse(link.containsKey("tenant_id"));
  }

  @Test
  void databaseIsolationAndEndpointVisibilityPreventCrossWorkspaceLinks() {
    var ops = sourced(OPS);
    var care = sourced(CARE);
    String privateId = object(ops, "PLACE");
    var careObject =
        request(
            CARE,
            care,
            "create_object",
            "object_type",
            "EVENT",
            "title",
            "Protected synthetic event",
            "summary",
            "An event in a different workspace.",
            "evidence_id",
            source(care));
    String ownId = (String) graph.command(CARE, key(), careObject).get("record_id");
    assertTrue(
        Json.rows(graph.graph(CARE).get("objects")).stream()
            .noneMatch(r -> privateId.equals(r.get("id"))));
    var invalid =
        request(
            CARE,
            care,
            "create_link",
            "from_id",
            ownId,
            "to_id",
            privateId,
            "relation",
            "LOCATED_AT",
            "assessment",
            "REPORTED",
            "reason",
            "A synthetic attempted cross-workspace link.",
            "evidence_id",
            source(care));
    code("NOT_FOUND", () -> graph.command(CARE, key(), invalid));
    for (String table :
        List.of("investigation_objects", "investigation_links", "investigation_link_reviews"))
      assertEquals(0, db.queryForObject("select count(*) from " + table, Integer.class));
    var wrongSource = objectRequest(ops, "ASSET");
    wrongSource.put("evidence_id", source(care));
    code("NOT_FOUND", () -> graph.command(OPS, key(), wrongSource));
  }

  @Test
  void readOnlyGraphAccessIsPurposeLoggedAndCannotWrite() {
    var mission = sourced(OPS);
    graph.graph(REVIEW);
    code("POLICY_DENIED", () -> graph.command(REVIEW, key(), objectRequest(mission, "PLACE")));
    assertTrue(
        repository.scoped(
                OPS,
                () ->
                    db.queryForObject(
                        "select count(*) from mission_access where actor='reviewer' and purpose='MISSION_REVIEW' and operation='GRAPH_READ'",
                        Integer.class))
            > 0);
  }

  @Test
  void forgedFieldsBadTypesAndInvalidOccurrenceTimesFailAtomically() {
    var mission = sourced(OPS);
    var request = objectRequest(mission, "PLACE");
    request.put("tenant_id", "care-demo");
    code("VALIDATION_FAILED", () -> graph.command(OPS, key(), request));
    request.remove("tenant_id");
    request.put("occurred_at", "2026-09-26T09:00:00Z");
    code("VALIDATION_FAILED", () -> graph.command(OPS, key(), request));
    request.put("object_type", "EVENT");
    request.put("occurred_at", "2026-09-26T09:00:00");
    code("VALIDATION_FAILED", () -> graph.command(OPS, key(), request));
    request.remove("occurred_at");
    request.put("object_type", "RISK_SCORE");
    code("VALIDATION_FAILED", () -> graph.command(OPS, key(), request));
    assertEquals(
        mission.get("version"), repository.get(OPS, (String) mission.get("id")).get("version"));
  }

  @Test
  void invalidSelfAndDuplicateRelationshipsAreRejected() {
    var mission = sourced(OPS);
    String a = object(mission, "PLACE"), b = object(mission, "ASSET");
    var wrong = linkRequest(mission, a, b);
    wrong.put("relation", "LOCATED_AT");
    code("INVALID_RELATION", () -> graph.command(OPS, key(), wrong));
    code("VALIDATION_FAILED", () -> graph.command(OPS, key(), linkRequest(mission, a, a)));
    graph.command(OPS, key(), linkRequest(mission, a, b));
    code("LINK_EXISTS", () -> graph.command(OPS, key(), linkRequest(mission, a, b)));
  }

  @Test
  void retryReturnsSameReceiptAndRejectsChangedPayload() {
    var mission = sourced(OPS);
    var body = objectRequest(mission, "PLACE");
    String key = key();
    var result = graph.command(OPS, key, body);
    assertEquals(result, graph.command(OPS, key, body));
    body.put("title", "Changed object title");
    code("IDEMPOTENCY_CONFLICT", () -> graph.command(OPS, key, body));
    assertEquals(3, repository.audit(OPS, (String) mission.get("id")).get("event_count"));
  }

  @Test
  void sourceChangesInvalidateClaimsUntilExplicitReviewPreservingHistory() {
    var mission = sourced(OPS);
    String from = object(mission, "ASSET"), to = object(mission, "PLACE");
    String id = (String) graph.command(OPS, key(), linkRequest(mission, from, to)).get("record_id");
    var before = record("links", id);
    var latest = repository.get(OPS, (String) mission.get("id"));
    service.apply(
        OPS,
        (String) mission.get("id"),
        key(),
        Json.object(
            "type",
            "review_evidence",
            "expected_version",
            latest.get("version"),
            "evidence_id",
            source(mission),
            "status",
            "DISPUTED",
            "statement",
            "The prior location claim is disputed.",
            "note",
            "A conflicting document requires reconciliation."));
    var stale = record("links", id);
    assertEquals(true, ((Map<?, ?>) stale.get("provenance")).get("needs_review"));
    var review =
        request(
            OPS,
            mission,
            "review_link",
            "link_id",
            id,
            "expected_revision",
            1,
            "assessment",
            "DISPUTED",
            "reason",
            "The revised source disputes the location claim.");
    graph.command(OPS, key(), review);
    var after = record("links", id);
    assertEquals(false, ((Map<?, ?>) after.get("provenance")).get("needs_review"));
    assertEquals(true, ((Map<?, ?>) record("objects", from).get("provenance")).get("needs_review"));
    assertEquals(before.get("original_sha256"), after.get("original_sha256"));
    assertEquals(2, Json.rows(after.get("history")).size());
    assertEquals("REPORTED", Json.rows(after.get("history")).getFirst().get("assessment"));
    review.put("expected_version", repository.get(OPS, (String) mission.get("id")).get("version"));
    code("VERSION_CONFLICT", () -> graph.command(OPS, key(), review));
    assertEquals(true, repository.audit(OPS, (String) mission.get("id")).get("verified"));
  }

  @Test
  void databaseRejectsRewritingObjectsAndAssessmentHistory() {
    var mission = sourced(OPS);
    String from = object(mission, "ASSET"), to = object(mission, "PLACE");
    String id = (String) graph.command(OPS, key(), linkRequest(mission, from, to)).get("record_id");
    assertThrows(
        DataAccessException.class,
        () ->
            repository.scoped(
                OPS,
                () ->
                    db.update(
                        "update investigation_objects set title='Rewritten source' where id=?",
                        from)));
    assertThrows(
        DataAccessException.class,
        () ->
            repository.scoped(
                OPS,
                () -> db.update("delete from investigation_link_reviews where link_id=?", id)));
  }

  @Test
  void completedMissionsCannotAcquireNewAssertions() {
    var mission = sourced(OPS);
    var request = objectRequest(mission, "PLACE");
    repository.scoped(
        OPS,
        () -> {
          mission.put("status", "COMPLETED");
          repository.save(mission);
          return null;
        });
    code("MISSION_COMPLETED", () -> graph.command(OPS, key(), request));
  }

  @Test
  void concurrentCommandsHaveOneWinnerWithoutPartialRecords() throws Exception {
    var mission = sourced(OPS);
    var body = objectRequest(mission, "PLACE");
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var work =
          (java.util.concurrent.Callable<String>)
              () -> {
                try {
                  graph.command(OPS, key(), body);
                  return "OK";
                } catch (Problem p) {
                  return p.code;
                }
              };
      var first = pool.submit(work);
      var second = pool.submit(work);
      assertEquals(
          java.util.Set.of("OK", "VERSION_CONFLICT"), java.util.Set.of(first.get(), second.get()));
    }
    assertEquals(
        1,
        Json.rows(graph.graph(OPS).get("objects")).stream()
            .filter(o -> mission.get("id").equals(o.get("mission_id")))
            .count());
    assertEquals(3, repository.audit(OPS, (String) mission.get("id")).get("event_count"));
  }
}
