// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Review integrity and
// isolation
package africa.vigil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class ExtractionIntegrationTest {
  @Autowired ExtractionService extracts;
  @Autowired MissionService service;
  @Autowired MissionRepository repository;
  @Autowired JdbcTemplate db;
  @MockitoBean MindClient mind;
  @MockitoSpyBean InvestigationService graph;
  static final Policy.Context OPS = Policy.identity("operations");
  static final Policy.Context CARE = Policy.identity("protection");
  static final Policy.Context REVIEW = Policy.identity("reviewer");
  static final String ORIGINAL =
      "  Municipal Pump 4 is located at Station North.\nSynthetic inspection.\n";

  @BeforeEach
  void model() {
    when(mind.extract(any(), any(), any())).thenAnswer(call -> output());
  }

  private static String key() {
    return UUID.randomUUID().toString();
  }

  private Map<String, Object> sourced() {
    var mission =
        service.create(
            OPS,
            key(),
            Json.object(
                "domain",
                "INVESTIGATE",
                "title",
                "Synthetic source review",
                "objective",
                "Verify publication of quoted source proposals.",
                "location",
                "Fictional district",
                "priority",
                "STANDARD"));
    return service.apply(
        OPS,
        (String) mission.get("id"),
        key(),
        Json.object(
            "type",
            "add_evidence",
            "expected_version",
            mission.get("version"),
            "title",
            "Synthetic inspection",
            "source",
            "Test custodian",
            "kind",
            "DOCUMENT",
            "statement",
            "An unverified synthetic source.",
            "original",
            ORIGINAL));
  }

  private Map<String, Object> output() {
    return Json.object(
        "model",
        "test-reasoner",
        "proposal",
        Json.object(
            "objects",
            List.of(
                Json.object(
                    "id",
                    "o1",
                    "object_type",
                    "ASSET",
                    "title",
                    "Municipal Pump 4",
                    "summary",
                    "The source describes a pump.",
                    "quote",
                    "Municipal Pump 4"),
                Json.object(
                    "id",
                    "o2",
                    "object_type",
                    "PLACE",
                    "title",
                    "Station North",
                    "summary",
                    "The source names this station.",
                    "quote",
                    "Station North")),
            "links",
            List.of(
                Json.object(
                    "id",
                    "l1",
                    "from_id",
                    "o1",
                    "to_id",
                    "o2",
                    "relation",
                    "LOCATED_AT",
                    "reason",
                    "The source places the pump at this station.",
                    "quote",
                    "Municipal Pump 4 is located at Station North.")),
            "gaps",
            List.of("Independent confirmation missing.")),
        "review",
        Json.object("state", "NOT_RUN", "model", null, "concerns", List.of()),
        "warnings",
        List.of());
  }

  private Map<String, Object> request(Map<String, Object> mission) {
    return Json.object(
        "mission_id",
        mission.get("id"),
        "evidence_id",
        Json.rows(mission.get("evidence")).getFirst().get("id"),
        "expected_version",
        mission.get("version"));
  }

  private Map<String, Object> propose(Map<String, Object> mission) {
    return extracts.propose(OPS, key(), request(mission));
  }

  private String runId(Map<String, Object> result) {
    return (String) Json.map(result.get("run")).get("id");
  }

  private Map<String, Object> reviewRequest(Map<String, Object> result) {
    return Json.object(
        "expected_version",
        Json.map(result.get("mission")).get("version"),
        "object_ids",
        List.of("o1", "o2"),
        "link_ids",
        List.of("l1"),
        "note",
        "Checked both quotations; retained as reported, pending verification.");
  }

  private void code(String expected, Runnable work) {
    assertEquals(expected, assertThrows(Problem.class, work::run).code);
  }

  private long count(String table, Map<String, Object> mission) {
    return repository.scoped(
        OPS,
        () ->
            db.queryForObject(
                "select count(*) from " + table + " where mission_id=?",
                Long.class,
                mission.get("id")));
  }

  @Test
  void publishedRecordsHaveOriginDecisionAndOneVerifiedAuditReceipt() {
    var mission = sourced();
    var source = Json.rows(mission.get("evidence")).getFirst();
    assertEquals(ORIGINAL, source.get("original"));
    assertEquals(Json.hash(ORIGINAL), source.get("sha256"));
    var proposed = propose(mission);
    assertEquals(0, count("investigation_objects", mission));
    var reviewed = extracts.review(OPS, runId(proposed), key(), reviewRequest(proposed));
    assertEquals(2, count("investigation_objects", mission));
    assertEquals(1, count("investigation_links", mission));
    var decision = Json.map(Json.map(reviewed.get("run")).get("decision"));
    assertEquals(Set.of("o1", "o2"), Json.map(decision.get("object_records")).keySet());
    String linkId = (String) Json.map(decision.get("link_records")).get("l1");
    var link =
        Json.rows(graph.graph(OPS).get("links")).stream()
            .filter(l -> linkId.equals(l.get("id")))
            .findFirst()
            .orElseThrow();
    assertEquals("REPORTED", link.get("assessment"));
    assertEquals(source.get("sha256"), link.get("original_sha256"));
    assertEquals(4, repository.audit(OPS, (String) mission.get("id")).get("event_count"));
    assertEquals(true, repository.audit(OPS, (String) mission.get("id")).get("verified"));
    var exported = Json.rows(repository.export(OPS, (String) mission.get("id")).get("extractions"));
    assertEquals(decision, exported.getFirst().get("decision"));
    assertEquals(
        Json.map(proposed.get("run")).get("proposal"), exported.getFirst().get("proposal"));
  }

  @Test
  void invalidModelQuotesTypesIdsAndAuthorityNeverPersist() {
    var mission = sourced();
    for (String invalid : List.of("quote", "pair", "endpoint", "duplicate", "authority", "empty")) {
      var raw = output();
      var proposal = Json.map(raw.get("proposal"));
      var object = Json.rows(proposal.get("objects")).getFirst();
      var link = Json.rows(proposal.get("links")).getFirst();
      switch (invalid) {
        case "quote" -> object.put("quote", "Invented source quotation");
        case "pair" -> link.put("relation", "SUPPLIED_BY");
        case "endpoint" -> link.put("to_id", "missing");
        case "duplicate" -> proposal.put("objects", List.of(object, object));
        case "authority" -> link.put("assessment", "CORROBORATED");
        default -> raw.clear();
      }
      when(mind.extract(any(), any(), any())).thenReturn(raw);
      code("INVALID_EXTRACTION", () -> propose(mission));
    }
    assertEquals(0, count("extraction_runs", mission));
    assertEquals(
        mission.get("version"), repository.get(OPS, (String) mission.get("id")).get("version"));
  }

  @Test
  void selectionRequiresKnownUniqueIdsAndBothEndpoints() {
    var mission = sourced();
    var proposed = propose(mission);
    var request = reviewRequest(proposed);
    request.put("object_ids", List.of("o1"));
    code("MISSING_ENDPOINT", () -> extracts.review(OPS, runId(proposed), key(), request));
    request.put("object_ids", List.of("o1", "missing"));
    code("INVALID_SELECTION", () -> extracts.review(OPS, runId(proposed), key(), request));
    request.put("object_ids", List.of("o1", "o1"));
    code("INVALID_SELECTION", () -> extracts.review(OPS, runId(proposed), key(), request));
    assertEquals(0, count("investigation_objects", mission));
    assertNull(
        Json.rows(extracts.list(OPS, (String) mission.get("id")).get("runs"))
            .getFirst()
            .get("decision"));
  }

  @Test
  void failureAfterObjectCreationRollsBackRecordsDecisionAndAudit() {
    var mission = sourced();
    var proposed = propose(mission);
    doThrow(new Problem(409, "TEST_FAILURE", "Synthetic transactional failure"))
        .when(graph)
        .createLink(any(), any(), any());
    code(
        "TEST_FAILURE",
        () -> extracts.review(OPS, runId(proposed), key(), reviewRequest(proposed)));
    assertEquals(0, count("investigation_objects", mission));
    assertEquals(0, count("investigation_links", mission));
    assertNull(
        Json.rows(extracts.list(OPS, (String) mission.get("id")).get("runs"))
            .getFirst()
            .get("decision"));
    assertEquals(3, repository.audit(OPS, (String) mission.get("id")).get("event_count"));
  }

  @Test
  void idempotencyAndOneProposalPerSnapshotPreventDuplicatePublication() {
    var mission = sourced();
    var body = request(mission);
    String extractionKey = key();
    var proposed = extracts.propose(OPS, extractionKey, body);
    assertEquals(proposed, extracts.propose(OPS, extractionKey, body));
    verify(mind, times(1)).extract(any(), any(), any());
    code("EXTRACTION_EXISTS", () -> propose(Json.map(proposed.get("mission"))));
    String reviewKey = key();
    var review = reviewRequest(proposed);
    var reviewed = extracts.review(OPS, runId(proposed), reviewKey, review);
    assertEquals(reviewed, extracts.review(OPS, runId(proposed), reviewKey, review));
    code("ALREADY_REVIEWED", () -> extracts.review(OPS, runId(proposed), key(), review));
    review.put("note", "Changed after final review.");
    code("IDEMPOTENCY_CONFLICT", () -> extracts.review(OPS, runId(proposed), reviewKey, review));
    assertEquals(2, count("investigation_objects", mission));
  }

  @Test
  void rejectAllIsAnImmutableRecordedDecision() {
    var mission = sourced();
    var proposed = propose(mission);
    var review = reviewRequest(proposed);
    review.put("object_ids", List.of());
    review.put("link_ids", List.of());
    extracts.review(OPS, runId(proposed), key(), review);
    assertEquals(0, count("investigation_objects", mission));
    assertNotNull(
        Json.rows(extracts.list(OPS, (String) mission.get("id")).get("runs"))
            .getFirst()
            .get("decision"));
    assertThrows(
        DataAccessException.class,
        () ->
            repository.scoped(
                OPS,
                () ->
                    db.update(
                        "update extraction_runs set decision='{}'::jsonb where id=?",
                        runId(proposed))));
    assertThrows(
        DataAccessException.class,
        () ->
            repository.scoped(
                OPS,
                () ->
                    db.update(
                        "update extraction_runs set body='{}'::jsonb where id=?",
                        runId(proposed))));
  }

  @Test
  void changedEvidenceAndClosedMissionsBlockPublication() {
    var mission = sourced();
    var proposed = propose(mission);
    var current = Json.map(proposed.get("mission"));
    var changed =
        service.apply(
            OPS,
            (String) mission.get("id"),
            key(),
            Json.object(
                "type",
                "review_evidence",
                "expected_version",
                current.get("version"),
                "evidence_id",
                Json.rows(current.get("evidence")).getFirst().get("id"),
                "status",
                "DISPUTED",
                "statement",
                "A conflicting location has been reported.",
                "note",
                "Synthetic contradiction requires review."));
    var review = reviewRequest(proposed);
    code("VERSION_CONFLICT", () -> extracts.review(OPS, runId(proposed), key(), review));
    review.put("expected_version", changed.get("version"));
    code("SOURCE_CHANGED", () -> extracts.review(OPS, runId(proposed), key(), review));
    assertEquals(
        true,
        Json.rows(extracts.list(OPS, (String) mission.get("id")).get("runs"))
            .getFirst()
            .get("stale"));
    var fresh = propose(changed);
    repository.scoped(
        OPS,
        () -> {
          var closed = Json.map(fresh.get("mission"));
          closed.put("status", "COMPLETED");
          repository.save(closed);
          return null;
        });
    code(
        "MISSION_COMPLETED", () -> extracts.review(OPS, runId(fresh), key(), reviewRequest(fresh)));
    assertEquals(0, count("investigation_objects", mission));
  }

  @Test
  void stateChangedDuringModelCallPreventsSavingTheProposal() {
    var mission = sourced();
    when(mind.extract(any(), any(), any()))
        .thenAnswer(
            call -> {
              service.apply(
                  OPS,
                  (String) mission.get("id"),
                  key(),
                  Json.object(
                      "type",
                      "review_evidence",
                      "expected_version",
                      mission.get("version"),
                      "evidence_id",
                      Json.rows(mission.get("evidence")).getFirst().get("id"),
                      "status",
                      "DISPUTED",
                      "statement",
                      "Changed while the model was running.",
                      "note",
                      "Concurrent synthetic source revision."));
              return output();
            });
    code("VERSION_CONFLICT", () -> propose(mission));
    assertEquals(0, count("extraction_runs", mission));
  }

  @Test
  void tenantIsolationReadOnlyAccessAndNoContextAreEnforced() {
    var mission = sourced();
    var proposed = propose(mission);
    assertEquals(
        1, Json.rows(extracts.list(REVIEW, (String) mission.get("id")).get("runs")).size());
    code("POLICY_DENIED", () -> extracts.propose(REVIEW, key(), request(mission)));
    code(
        "POLICY_DENIED",
        () -> extracts.review(REVIEW, runId(proposed), key(), reviewRequest(proposed)));
    code("NOT_FOUND", () -> extracts.list(CARE, (String) mission.get("id")));
    code("NOT_FOUND", () -> extracts.review(CARE, runId(proposed), key(), reviewRequest(proposed)));
    assertEquals(0, db.queryForObject("select count(*) from extraction_runs", Integer.class));
  }

  @Test
  void concurrentReviewsHaveOneWinnerWithoutDuplicateRecords() throws Exception {
    var mission = sourced();
    var proposed = propose(mission);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
      var work =
          (java.util.concurrent.Callable<String>)
              () -> {
                try {
                  extracts.review(OPS, runId(proposed), key(), reviewRequest(proposed));
                  return "OK";
                } catch (Problem p) {
                  return p.code;
                }
              };
      var a = pool.submit(work);
      var b = pool.submit(work);
      assertEquals(Set.of("OK", "ALREADY_REVIEWED"), Set.of(a.get(), b.get()));
    }
    assertEquals(2, count("investigation_objects", mission));
    assertEquals(1, count("investigation_links", mission));
  }
}
