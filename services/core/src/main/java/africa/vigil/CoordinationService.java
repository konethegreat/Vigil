// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Explicit shared cases, evidence
// release and accountable requests
package africa.vigil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
final class CoordinationService {
  private final MissionRepository repo;
  private final ObservationService observations;
  private final JdbcTemplate db;

  CoordinationService(MissionRepository repo, ObservationService observations, JdbcTemplate db) {
    this.repo = repo;
    this.observations = observations;
    this.db = db;
  }

  private void agency(Policy.Context context) {
    Policy.requireContext(context);
    Problem.require(
        Policy.agencies().contains(context.tenant()),
        403,
        "AGENCY_REQUIRED",
        "Choose a synthetic regional agency workspace.");
  }

  Map<String, Object> workspace(Policy.Context context) {
    return repo.scoped(
        context,
        () -> {
          observations.access(context, "coordination", "WORKSPACE_READ");
          return Json.object(
              "identity",
              context.publicView(),
              "agencies",
              Policy.agencies().stream()
                  .map(t -> Json.object("id", t, "name", Policy.organisation(t)))
                  .toList(),
              "sources",
              observations.sources(),
              "observations",
              observations.observations(),
              "cases",
              cases(),
              "requests",
              requests(null),
              "server_time",
              Instant.now().toString());
        });
  }

  private List<Map<String, Object>> cases() {
    return db.query(
        "select body::text from agency_cases order by id", (rs, n) -> Json.decode(rs.getString(1)));
  }

  private Map<String, Object> caseRecord(String id) {
    var rows =
        db.query(
            "select body::text from agency_cases where id=?",
            (rs, n) -> Json.decode(rs.getString(1)),
            id);
    Problem.require(!rows.isEmpty(), 404, "NOT_FOUND", "Case unavailable in this workspace.");
    return rows.getFirst();
  }

  private List<Map<String, Object>> requests(String caseId) {
    return caseId == null
        ? db.query(
            "select body::text from agency_requests order by id",
            (rs, n) -> Json.decode(rs.getString(1)))
        : db.query(
            "select body::text from agency_requests where case_id=? order by id",
            (rs, n) -> Json.decode(rs.getString(1)),
            caseId);
  }

  private Map<String, Object> request(String id) {
    var rows =
        db.query(
            "select body::text from agency_requests where id=? for update",
            (rs, n) -> Json.decode(rs.getString(1)),
            id);
    Problem.require(!rows.isEmpty(), 404, "NOT_FOUND", "Request unavailable in this workspace.");
    return rows.getFirst();
  }

  Map<String, Object> createCase(Policy.Context context, String key, Map<String, Object> body) {
    agency(context);
    MissionRules.fields(
        body, Set.of("title", "objective", "location", "occurred_at", "peer_tenant", "purpose"));
    return repo.command(
        context,
        "agency-case",
        key,
        body,
        () -> {
          observations.lock(context);
          String peer =
              CoordinationRules.choice(body, "peer_tenant", Set.copyOf(Policy.agencies()));
          Problem.require(
              !peer.equals(context.tenant()), 400, "VALIDATION_FAILED", "Choose another agency.");
          Problem.require(
              db.queryForObject(
                      "select count(*) from agency_cases where owner_tenant=?",
                      Integer.class,
                      context.tenant())
                  < 100,
              409,
              "CAPACITY",
              "Case limit reached.");
          var record =
              Json.object(
                  "id",
                  CoordinationRules.id("CASE-"),
                  "title",
                  CoordinationRules.text(body, "title", 5, 160),
                  "objective",
                  CoordinationRules.text(body, "objective", 10, 1000),
                  "location",
                  CoordinationRules.text(body, "location", 3, 160),
                  "occurred_at",
                  CoordinationRules.instant(body, "occurred_at").toString(),
                  "purpose",
                  CoordinationRules.text(body, "purpose", 10, 500),
                  "owner_tenant",
                  context.tenant(),
                  "peer_tenant",
                  peer,
                  "created_by",
                  context.name(),
                  "created_at",
                  Instant.now().toString(),
                  "synthetic",
                  true);
          db.update(
              "insert into agency_cases(id,owner_tenant,peer_tenant,body) values(?,?,?,?::jsonb)",
              record.get("id"),
              context.tenant(),
              peer,
              Json.encode(record));
          observations.access(context, (String) record.get("id"), "CASE_CREATED");
          return Json.object("id", record.get("id"));
        });
  }

  Map<String, Object> details(Policy.Context context, String id, boolean export) {
    return repo.scoped(
        context,
        () -> {
          var record = caseRecord(id);
          observations.access(context, id, export ? "CASE_EXPORT" : "CASE_READ");
          var expanded = new ArrayList<Map<String, Object>>();
          for (var request : requests(id)) {
            String rid = (String) request.get("id");
            var packets =
                db.query(
                    "select body::text,fingerprint,revoked_at::text from agency_packets where request_id=? order by kind",
                    (rs, n) ->
                        Json.object(
                            "content",
                            Json.decode(rs.getString(1)),
                            "fingerprint",
                            rs.getString(2),
                            "revoked_at",
                            rs.getString(3)),
                    rid);
            var receipts = receipts(rid);
            String prior = "GENESIS";
            boolean valid = true;
            for (var event : receipts) {
              String hash = (String) event.remove("hash");
              valid &=
                  prior.equals(event.get("previous_hash"))
                      && Json.hash(Json.encode(event)).equals(hash);
              event.put("hash", hash);
              prior = hash;
            }
            request.put("packets", packets);
            request.put("receipts", receipts);
            request.put(
                "audit",
                Json.object(
                    "verified",
                    valid,
                    "head_hash",
                    prior,
                    "event_count",
                    receipts.size(),
                    "scope",
                    "Local request receipt chain; no external anchoring."));
            request.put("release_active", active(request) && !request.get("state").equals("DRAFT"));
            expanded.add(request);
          }
          return Json.object(
              "format",
              "vigil.coordination.case.v1",
              "synthetic",
              true,
              "case",
              record,
              "requests",
              expanded,
              "retrieved_at",
              Instant.now().toString());
        });
  }

  Map<String, Object> draft(Policy.Context context, String key, Map<String, Object> body) {
    agency(context);
    MissionRules.fields(
        body,
        Set.of(
            "case_id",
            "title",
            "task",
            "observation_ids",
            "due_at",
            "expires_at",
            "release_reason"));
    return repo.command(
        context,
        "agency-request",
        key,
        body,
        () -> {
          String cid = CoordinationRules.text(body, "case_id", 5, 80);
          var caseRecord = caseRecord(cid);
          db.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))", "case:" + cid);
          Problem.require(
              db.queryForObject(
                      "select count(*) from agency_requests where case_id=?", Integer.class, cid)
                  < 100,
              409,
              "CAPACITY",
              "Case request limit reached.");
          String recipient =
              (String)
                  (caseRecord.get("owner_tenant").equals(context.tenant())
                      ? caseRecord.get("peer_tenant")
                      : caseRecord.get("owner_tenant"));
          Instant due = CoordinationRules.instant(body, "due_at"),
              expires = CoordinationRules.instant(body, "expires_at"),
              now = Instant.now();
          Problem.require(
              due.isAfter(now)
                  && !expires.isBefore(due)
                  && !expires.isAfter(now.plusSeconds(30L * 86400)),
              400,
              "VALIDATION_FAILED",
              "Choose a future deadline and an expiry at or after it, within 30 days.");
          var selected = observations.snapshot(CoordinationRules.ids(body, "observation_ids"));
          var record =
              Json.object(
                  "id",
                  CoordinationRules.id("REQ-"),
                  "case_id",
                  cid,
                  "sender_tenant",
                  context.tenant(),
                  "recipient_tenant",
                  recipient,
                  "title",
                  CoordinationRules.text(body, "title", 5, 160),
                  "task",
                  CoordinationRules.text(body, "task", 10, 1000),
                  "due_at",
                  due.toString(),
                  "expires_at",
                  expires.toString(),
                  "release_reason",
                  CoordinationRules.text(body, "release_reason", 10, 1000),
                  "created_at",
                  now.toString(),
                  "state",
                  "DRAFT",
                  "version",
                  1,
                  "revoked_at",
                  null,
                  "accepted_by",
                  null,
                  "synthetic",
                  true);
          String id = (String) record.get("id");
          db.update(
              "insert into agency_requests(id,case_id,sender_tenant,recipient_tenant,body) values(?,?,?,?,?::jsonb)",
              id,
              cid,
              context.tenant(),
              recipient,
              Json.encode(record));
          packet(context, record, "REQUEST", recipient, selected);
          append(context, id, "DRAFT", "Draft created; selected evidence has not been released.");
          return receiptResult(record);
        });
  }

  private boolean active(Map<String, Object> request) {
    return request.get("revoked_at") == null
        && Instant.parse((String) request.get("expires_at")).isAfter(Instant.now())
        && !request.get("state").equals("CANCELLED");
  }

  Map<String, Object> command(
      Policy.Context context, String id, String key, Map<String, Object> body) {
    agency(context);
    String action = CoordinationRules.text(body, "action", 3, 40);
    MissionRules.fields(
        body,
        action.equals("RESULT_RECORDED")
            ? Set.of("expected_version", "action", "note", "observation_ids")
            : Set.of("expected_version", "action", "note"));
    return repo.command(
        context,
        "agency-request:" + id,
        key,
        body,
        () -> {
          var record = request(id);
          CoordinationRules.version(record, body);
          String note = CoordinationRules.text(body, "note", 10, 1500),
              state = (String) record.get("state");
          boolean sender = context.tenant().equals(record.get("sender_tenant"));
          boolean senderAction =
              Set.of("RELEASED", "CANCELLED", "REVOKED", "CLOSED").contains(action);
          Problem.require(
              sender == senderAction,
              403,
              "POLICY_DENIED",
              "This transition belongs to the other agency.");
          if (action.equals("REVOKED")) {
            Problem.require(
                record.get("revoked_at") == null && !state.equals("DRAFT"),
                409,
                "INVALID_TRANSITION",
                "Only an unrecalled release can be revoked.");
            revoke(record);
          } else if (action.equals("CANCELLED")) {
            Problem.require(
                !Set.of("CLOSED", "CANCELLED", "DECLINED", "RESULT_RECORDED").contains(state),
                409,
                "INVALID_TRANSITION",
                "This request cannot be cancelled.");
            revoke(record);
            record.put("state", action);
          } else {
            if (!action.equals("CLOSED"))
              Problem.require(
                  active(record),
                  409,
                  "RELEASE_UNAVAILABLE",
                  "The evidence release has expired or been revoked.");
            Set<String> next =
                switch (state) {
                  case "DRAFT" -> Set.of("RELEASED");
                  case "RELEASED" -> Set.of("DELIVERED");
                  case "DELIVERED" -> Set.of("ACCEPTED", "DECLINED");
                  case "ACCEPTED" -> Set.of("IN_PROGRESS");
                  case "IN_PROGRESS" -> Set.of("RESULT_RECORDED", "OUTCOME_UNKNOWN");
                  case "RESULT_RECORDED", "OUTCOME_UNKNOWN" -> Set.of("CLOSED");
                  default -> Set.of();
                };
            Problem.require(
                next.contains(action),
                409,
                "INVALID_TRANSITION",
                "Cannot move from " + state + " to " + action + ".");
            if (action.equals("ACCEPTED")) record.put("accepted_by", context.name());
            if (action.equals("RESULT_RECORDED"))
              packet(
                  context,
                  record,
                  "RESULT",
                  (String) record.get("sender_tenant"),
                  observations.snapshot(CoordinationRules.ids(body, "observation_ids")));
            record.put("state", action);
          }
          record.put("version", Json.number(record, "version") + 1);
          record.put("updated_at", Instant.now().toString());
          db.update(
              "update agency_requests set state=?,version=?,body=?::jsonb where id=?",
              record.get("state"),
              record.get("version"),
              Json.encode(record),
              id);
          append(context, id, action, note);
          return receiptResult(record);
        });
  }

  private void revoke(Map<String, Object> record) {
    String at = Instant.now().toString();
    db.update(
        "update agency_packets set revoked_at=?::timestamptz where request_id=? and kind='REQUEST' and revoked_at is null",
        at,
        record.get("id"));
    record.put("revoked_at", at);
  }

  private Map<String, Object> receiptResult(Map<String, Object> record) {
    return Json.object(
        "id",
        record.get("id"),
        "case_id",
        record.get("case_id"),
        "state",
        record.get("state"),
        "version",
        record.get("version"));
  }

  private void packet(
      Policy.Context context,
      Map<String, Object> request,
      String kind,
      String recipient,
      List<Map<String, Object>> selected) {
    var content =
        Json.object(
            "kind",
            kind,
            "request_id",
            request.get("id"),
            "owner_tenant",
            context.tenant(),
            "recipient_tenant",
            recipient,
            "expires_at",
            request.get("expires_at"),
            "released_by",
            context.name(),
            "prepared_at",
            Instant.now().toString(),
            "observations",
            selected,
            "synthetic",
            true);
    db.update(
        "insert into agency_packets(id,request_id,kind,owner_tenant,recipient_tenant,expires_at,body,fingerprint) values(?,?,?,?,?,?::timestamptz,?::jsonb,?)",
        CoordinationRules.id("PKT-"),
        request.get("id"),
        kind,
        context.tenant(),
        recipient,
        request.get("expires_at"),
        Json.encode(content),
        Json.hash(Json.encode(content)));
  }

  private List<Map<String, Object>> receipts(String id) {
    return db.query(
        "select body::text from agency_receipts where request_id=? order by sequence",
        (rs, n) -> Json.decode(rs.getString(1)),
        id);
  }

  void append(Policy.Context context, String id, String kind, String note) {
    var previous = receipts(id);
    String prior = previous.isEmpty() ? "GENESIS" : (String) previous.getLast().get("hash");
    var event =
        Json.object(
            "id",
            CoordinationRules.id("ACK-"),
            "request_id",
            id,
            "kind",
            kind,
            "note",
            note,
            "actor",
            context.name(),
            "agency",
            context.tenant(),
            "at",
            Instant.now().toString(),
            "previous_hash",
            prior);
    event.put("hash", Json.hash(Json.encode(event)));
    db.update(
        "insert into agency_receipts(request_id,actor_tenant,body) values(?,?,?::jsonb)",
        id,
        context.tenant(),
        Json.encode(event));
  }
}
