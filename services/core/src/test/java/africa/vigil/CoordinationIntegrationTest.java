// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Connected operations isolation,
// replay and results
package africa.vigil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class CoordinationIntegrationTest {
  @Autowired ObservationService obs;
  @MockitoSpyBean CoordinationService cases;
  @Autowired MissionRepository repo;
  @Autowired JdbcTemplate db;
  static final Policy.Context GP = Policy.identity("gauteng"),
      LP = Policy.identity("limpopo"),
      OTHER = Policy.identity("protection"),
      REVIEW = Policy.identity("agency-reviewer");

  static String key() {
    return UUID.randomUUID().toString();
  }

  Map<String, Object> source(Policy.Context context, String status) {
    return obs.register(
        context,
        key(),
        Json.object(
            "name",
            "Synthetic source " + key(),
            "kind",
            "CAMERA",
            "location",
            "Fictional corridor",
            "latitude",
            -25.1,
            "longitude",
            28.5,
            "capability",
            "Manual synthetic observation records; no live feed.",
            "coverage_from",
            "2026-09-01T00:00:00Z",
            "coverage_to",
            "2026-10-01T00:00:00Z",
            "status",
            status,
            "status_note",
            "Declared synthetic coverage for testing."));
  }

  Map<String, Object> event(Map<String, Object> source) {
    return Json.object(
        "source_id",
        source.get("id"),
        "producer_event_id",
        key(),
        "occurred_at",
        "2026-09-26T17:20:00Z",
        "clock_uncertainty_seconds",
        120,
        "description",
        "A synthetic vehicle description; identity unconfirmed.",
        "original",
        "  SYNTHETIC: a grey delivery vehicle appears.\r\nNo driver identity established.\r\n");
  }

  Map<String, Object> observation(Policy.Context context) {
    return obs.ingest(context, key(), event(source(context, "AVAILABLE")));
  }

  String createCase() {
    return (String)
        cases
            .createCase(
                GP,
                key(),
                Json.object(
                    "title",
                    "Synthetic corridor inquiry",
                    "objective",
                    "Verify an observation and return a supported result.",
                    "location",
                    "Fictional Gauteng corridor",
                    "occurred_at",
                    "2026-09-26T17:00:00Z",
                    "purpose",
                    "Review selected observations for this synthetic incident.",
                    "peer_tenant",
                    LP.tenant()))
            .get("id");
  }

  Map<String, Object> draft(String caseId, String observationId, Instant expiry) {
    return cases.draft(
        GP,
        key(),
        Json.object(
            "case_id",
            caseId,
            "title",
            "Verify a reported sighting",
            "task",
            "Inspect the selected evidence and report what the source establishes.",
            "observation_ids",
            List.of(observationId),
            "due_at",
            expiry.minusMillis(200).toString(),
            "expires_at",
            expiry.toString(),
            "release_reason",
            "Selected source is relevant to this synthetic case; no attribution is established."));
  }

  Map<String, Object> draft() {
    return draft(createCase(), (String) observation(GP).get("id"), Instant.now().plusSeconds(3600));
  }

  Map<String, Object> transition(
      Policy.Context context, Map<String, Object> request, String action) {
    return cases.command(
        context,
        (String) request.get("id"),
        key(),
        Json.object(
            "expected_version",
            request.get("version"),
            "action",
            action,
            "note",
            "Synthetic recorded explanation for " + action + "."));
  }

  Map<String, Object> detail(Policy.Context context, Map<String, Object> request) {
    return Json.rows(cases.details(context, (String) request.get("case_id"), false).get("requests"))
        .stream()
        .filter(r -> r.get("id").equals(request.get("id")))
        .findFirst()
        .orElseThrow();
  }

  void fails(int status, Runnable work) {
    assertEquals(status, assertThrows(Problem.class, work::run).status);
  }

  @Test
  void observationsPreserveOriginalAndDeduplicateProducerAcrossCommandKeys() {
    var body = event(source(GP, "AVAILABLE"));
    String key = key();
    var first = obs.ingest(GP, key, body);
    assertEquals(first, obs.ingest(GP, key, body));
    assertEquals(first, obs.ingest(GP, key(), body));
    assertEquals(body.get("original"), first.get("original"));
    assertEquals(Json.hash((String) body.get("original")), first.get("sha256"));
    assertNotEquals(first.get("occurred_at"), first.get("received_at"));
    assertTrue(
        ((List<?>) first.get("flags")).containsAll(List.of("LATE_ARRIVAL", "CLOCK_UNCERTAINTY")));
    body.put("description", "Conflicting content for the same producer event.");
    fails(409, () -> obs.ingest(GP, key(), body));
  }

  @Test
  void requestCapacityIsPerOriginatingAgencyRegardlessOfPeerVisibility() {
    String cid = createCase();
    String oid = (String) observation(GP).get("id");
    Instant expires = Instant.now().plusSeconds(3600);
    for (int i = 0; i < 100; i++) {
      transition(GP, draft(cid, oid, expires), "RELEASED");
    }
    fails(409, () -> draft(cid, oid, expires));
    var receiving =
        cases.draft(
            LP,
            key(),
            Json.object(
                "case_id",
                cid,
                "title",
                "Independent receiving-team request",
                "task",
                "Review the selected receiving-team source independently.",
                "observation_ids",
                List.of(observation(LP).get("id")),
                "due_at",
                expires.minusSeconds(60).toString(),
                "expires_at",
                expires.toString(),
                "release_reason",
                "Selected fictional record requires peer review."));
    assertEquals("DRAFT", receiving.get("state"));
  }

  @Test
  void observationIsolationCorrectionsAndImmutability() {
    var original = observation(GP);
    repo.scoped(
        LP,
        () -> {
          fails(404, () -> obs.observation((String) original.get("id")));
          return null;
        });
    var body = event(Json.object("id", original.get("source_id")));
    body.put("supersedes_id", original.get("id"));
    var correction = obs.ingest(GP, key(), body);
    assertEquals(original.get("id"), correction.get("supersedes_id"));
    body.put("producer_event_id", key());
    fails(409, () -> obs.ingest(GP, key(), body));
    assertThrows(
        DataAccessException.class,
        () ->
            repo.scoped(
                GP,
                () ->
                    db.update(
                        "update observations set body='{}'::jsonb where id=?",
                        original.get("id"))));
  }

  @Test
  void coverageNeverEquatesOfflineOrMissingPeriodWithNoMatch() {
    var offline = source(GP, "OFFLINE");
    var available = source(GP, "AVAILABLE");
    var body =
        Json.object(
            "from",
            "2026-09-26T17:00:00Z",
            "to",
            "2026-09-26T18:00:00Z",
            "query",
            "no matching fictional term",
            "source_id",
            offline.get("id"));
    var result = obs.search(REVIEW, body);
    assertEquals("OFFLINE", Json.rows(result.get("coverage")).getFirst().get("outcome"));
    body.put("source_id", available.get("id"));
    assertEquals(
        "NO_MATCH_IN_DECLARED_COVERAGE",
        Json.rows(obs.search(GP, body).get("coverage")).getFirst().get("outcome"));
    body.put("to", "2026-10-02T00:00:00Z");
    assertEquals(
        "PARTIAL", Json.rows(obs.search(GP, body).get("coverage")).getFirst().get("outcome"));
    body.put("from", "2026-08-01T00:00:00Z");
    fails(400, () -> obs.search(GP, body));
  }

  @Test
  void searchIncludesAnOverlappingUncertainClockInterval() {
    var source = source(GP, "AVAILABLE");
    var observation = obs.ingest(GP, key(), event(source));
    var query =
        Json.object(
            "source_id",
            source.get("id"),
            "from",
            "2026-09-26T17:21:00Z",
            "to",
            "2026-09-26T17:22:00Z");
    assertEquals(
        observation.get("id"),
        Json.rows(obs.search(GP, query).get("observations")).getFirst().get("id"));
    query.put("from", "2026-09-26T17:23:00Z");
    query.put("to", "2026-09-26T17:24:00Z");
    assertTrue(Json.rows(obs.search(GP, query).get("observations")).isEmpty());
  }

  @Test
  void statusChangesUseVersionAndRejectReadonlyAndForgedAuthority() {
    var source = source(GP, "AVAILABLE");
    var body =
        Json.object(
            "expected_version",
            1,
            "status",
            "OFFLINE",
            "status_note",
            "Synthetic connection unavailable.");
    fails(403, () -> obs.status(REVIEW, (String) source.get("id"), key(), body));
    assertEquals("OFFLINE", obs.status(GP, (String) source.get("id"), key(), body).get("status"));
    fails(409, () -> obs.status(GP, (String) source.get("id"), key(), body));
    body.put("tenant_id", LP.tenant());
    fails(400, () -> obs.status(GP, (String) source.get("id"), key(), body));
    var invalid = event(source);
    invalid.put("clock_uncertainty_seconds", 1.5);
    fails(400, () -> obs.ingest(GP, key(), invalid));
  }

  @Test
  void draftPrivateAndPacketReleaseDoesNotExposeSourceRegister() {
    var request = draft();
    String cid = (String) request.get("case_id");
    assertTrue(Json.rows(cases.details(LP, cid, false).get("requests")).isEmpty());
    fails(404, () -> cases.details(OTHER, cid, true));
    fails(404, () -> transition(LP, request, "DELIVERED"));
    var released = transition(GP, request, "RELEASED");
    var packet = Json.map(Json.rows(detail(LP, released).get("packets")).getFirst().get("content"));
    var shared = Json.rows(packet.get("observations")).getFirst();
    repo.scoped(
        LP,
        () -> {
          fails(404, () -> obs.observation((String) shared.get("id")));
          fails(404, () -> obs.source((String) shared.get("source_id")));
          return null;
        });
  }

  @Test
  void completeTwoAgencyWorkflowReturnsEvidenceAndVerifiableReceipts() {
    var request = transition(GP, draft(), "RELEASED");
    request = transition(LP, request, "DELIVERED");
    request = transition(LP, request, "ACCEPTED");
    request = transition(LP, request, "IN_PROGRESS");
    var result = observation(LP);
    request =
        cases.command(
            LP,
            (String) request.get("id"),
            key(),
            Json.object(
                "expected_version",
                request.get("version"),
                "action",
                "RESULT_RECORDED",
                "note",
                "Independent synthetic source cannot confirm a driver or involvement.",
                "observation_ids",
                List.of(result.get("id"))));
    request = transition(GP, request, "CLOSED");
    var detail = detail(GP, request);
    assertEquals("CLOSED", detail.get("state"));
    assertEquals(LP.name(), detail.get("accepted_by"));
    assertEquals(2, Json.rows(detail.get("packets")).size());
    assertEquals(7, Json.map(detail.get("audit")).get("event_count"));
    assertEquals(true, Json.map(detail.get("audit")).get("verified"));
    assertTrue(
        Json.encode(cases.details(GP, (String) request.get("case_id"), true))
            .contains((String) result.get("id")));
  }

  @Test
  void receiptReplayIsMetadataOnlyAndConflictingKeysFail() {
    var draft = draft();
    String id = (String) draft.get("id"), key = key();
    var body =
        Json.object(
            "expected_version",
            1,
            "action",
            "RELEASED",
            "note",
            "Explicit synthetic evidence release.");
    var first = cases.command(GP, id, key, body);
    assertEquals(first, cases.command(GP, id, key, body));
    assertFalse(first.containsKey("packets"));
    assertEquals(2, Json.rows(detail(GP, first).get("receipts")).size());
    body.put("note", "A different explanation for the same command.");
    fails(409, () -> cases.command(GP, id, key, body));
    fails(403, () -> cases.command(REVIEW, id, key, body));
  }

  @Test
  void onlyCorrectAgencyCanPerformEachTransitionAndNoSkipping() {
    var request = draft();
    fails(403, () -> transition(REVIEW, request, "RELEASED"));
    fails(403, () -> transition(GP, request, "ACCEPTED"));
    var released = transition(GP, request, "RELEASED");
    fails(409, () -> transition(LP, released, "IN_PROGRESS"));
    fails(403, () -> transition(LP, released, "CLOSED"));
    var accepted = transition(LP, transition(LP, released, "DELIVERED"), "ACCEPTED");
    fails(409, () -> transition(LP, released, "DELIVERED"));
    assertEquals("ACCEPTED", detail(GP, accepted).get("state"));
  }

  @Test
  void revokedPacketDisappearsFromRecipientReadsAndExportAndBlocksWork() {
    var released = transition(GP, draft(), "RELEASED");
    var revoked = transition(GP, released, "REVOKED");
    assertTrue(Json.rows(detail(LP, revoked).get("packets")).isEmpty());
    assertEquals(1, Json.rows(detail(GP, revoked).get("packets")).size());
    assertFalse(
        Json.encode(cases.details(LP, (String) revoked.get("case_id"), true))
            .contains("No driver identity established"));
    fails(409, () -> transition(LP, revoked, "DELIVERED"));
    repo.scoped(
        LP,
        () -> {
          assertEquals(
              0,
              db.queryForObject(
                  "select count(*) from agency_packets where request_id=?",
                  Integer.class,
                  released.get("id")));
          return null;
        });
  }

  @Test
  void expiryIsEnforcedByDatabaseAndService() throws Exception {
    var observation = observation(GP);
    var cid = createCase();
    var request = draft(cid, (String) observation.get("id"), Instant.now().plusMillis(1800));
    var released = transition(GP, request, "RELEASED");
    Thread.sleep(1900);
    assertTrue(Json.rows(detail(LP, released).get("packets")).isEmpty());
    fails(409, () -> transition(LP, released, "DELIVERED"));
  }

  @Test
  void invalidResultRollsBackStateReceiptAndCommand() {
    var request =
        transition(
            LP,
            transition(
                LP, transition(LP, transition(GP, draft(), "RELEASED"), "DELIVERED"), "ACCEPTED"),
            "IN_PROGRESS");
    var wrong = observation(GP);
    String key = key();
    var body =
        Json.object(
            "expected_version",
            request.get("version"),
            "action",
            "RESULT_RECORDED",
            "note",
            "Synthetic completion requires recipient evidence.",
            "observation_ids",
            List.of(wrong.get("id")));
    fails(404, () -> cases.command(LP, (String) request.get("id"), key, body));
    assertEquals("IN_PROGRESS", detail(GP, request).get("state"));
    assertEquals(5, Json.rows(detail(GP, request).get("receipts")).size());
    body.put("observation_ids", List.of(observation(LP).get("id")));
    assertEquals(
        "RESULT_RECORDED", cases.command(LP, (String) request.get("id"), key, body).get("state"));
  }

  @Test
  void simultaneousAcknowledgmentsHaveOneWinner() throws Exception {
    var request = transition(GP, draft(), "RELEASED");
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var work =
          (java.util.concurrent.Callable<Integer>)
              () -> {
                try {
                  transition(LP, request, "DELIVERED");
                  return 200;
                } catch (Problem e) {
                  return e.status;
                }
              };
      var a = executor.submit(work);
      var b = executor.submit(work);
      assertEquals(
          List.of(200, 409), java.util.stream.Stream.of(a.get(), b.get()).sorted().toList());
    }
    assertEquals(3, Json.rows(detail(GP, request).get("receipts")).size());
  }

  @Test
  void failureAfterResultPacketInsertRollsBackAllWrites() {
    var request =
        transition(
            LP,
            transition(
                LP, transition(LP, transition(GP, draft(), "RELEASED"), "DELIVERED"), "ACCEPTED"),
            "IN_PROGRESS");
    String id = (String) request.get("id"), commandKey = key();
    var body =
        Json.object(
            "expected_version",
            request.get("version"),
            "action",
            "RESULT_RECORDED",
            "note",
            "Synthetic result requiring an atomic receipt.",
            "observation_ids",
            List.of(observation(LP).get("id")));
    doThrow(new IllegalStateException("Forced receipt failure"))
        .when(cases)
        .append(any(), eq(id), eq("RESULT_RECORDED"), anyString());
    try {
      assertThrows(IllegalStateException.class, () -> cases.command(LP, id, commandKey, body));
    } finally {
      doCallRealMethod().when(cases).append(any(), eq(id), eq("RESULT_RECORDED"), anyString());
    }
    assertEquals("IN_PROGRESS", detail(GP, request).get("state"));
    repo.scoped(
        LP,
        () -> {
          assertEquals(
              0,
              db.queryForObject(
                  "select count(*) from agency_packets where request_id=? and kind='RESULT'",
                  Integer.class,
                  id));
          return null;
        });
    assertEquals("RESULT_RECORDED", cases.command(LP, id, commandKey, body).get("state"));
  }

  @Test
  void packetsReceiptsAndMembershipCannotBeRewritten() {
    var request = transition(GP, draft(), "RELEASED");
    assertThrows(
        DataAccessException.class,
        () ->
            repo.scoped(
                GP,
                () ->
                    db.update(
                        "update agency_packets set body='{}' where request_id=?",
                        request.get("id"))));
    assertThrows(
        DataAccessException.class,
        () ->
            repo.scoped(
                GP,
                () ->
                    db.update(
                        "update agency_requests set recipient_tenant=? where id=?",
                        OTHER.tenant(),
                        request.get("id"))));
    assertThrows(
        DataAccessException.class,
        () ->
            repo.scoped(
                GP,
                () ->
                    db.update(
                        "delete from agency_receipts where request_id=?", request.get("id"))));
    fails(
        404,
        () -> cases.details(Policy.identity("operations"), (String) request.get("case_id"), false));
  }
}
