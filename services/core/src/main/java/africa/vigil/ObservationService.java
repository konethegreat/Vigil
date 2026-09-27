// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Immutable owned observations and
// honest coverage
package africa.vigil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
final class ObservationService {
  private final MissionRepository repo;
  private final JdbcTemplate db;

  ObservationService(MissionRepository repo, JdbcTemplate db) {
    this.repo = repo;
    this.db = db;
  }

  void lock(Policy.Context context) {
    db.queryForList(
        "select pg_advisory_xact_lock(hashtextextended(?,0))", "observations:" + context.tenant());
  }

  void access(Policy.Context context, String resource, String operation) {
    db.update(
        "insert into coordination_access(tenant_id,actor,purpose,resource,operation) values(?,?,?,?,?)",
        context.tenant(),
        context.actor(),
        context.purpose(),
        resource,
        operation);
  }

  List<Map<String, Object>> sources() {
    return db.query(
        "select body::text from observation_sources order by id",
        (rs, n) -> Json.decode(rs.getString(1)));
  }

  List<Map<String, Object>> observations() {
    return db.query(
        "select body::text from observations order by occurred_at,id",
        (rs, n) -> Json.decode(rs.getString(1)));
  }

  Map<String, Object> source(String id) {
    var rows =
        db.query(
            "select body::text from observation_sources where id=?",
            (rs, n) -> Json.decode(rs.getString(1)),
            id);
    Problem.require(!rows.isEmpty(), 404, "NOT_FOUND", "Source unavailable in this workspace.");
    return rows.getFirst();
  }

  Map<String, Object> observation(String id) {
    var rows =
        db.query(
            "select body::text from observations where id=?",
            (rs, n) -> Json.decode(rs.getString(1)),
            id);
    Problem.require(
        !rows.isEmpty(), 404, "NOT_FOUND", "Observation unavailable in this workspace.");
    return rows.getFirst();
  }

  Map<String, Object> register(Policy.Context context, String key, Map<String, Object> body) {
    MissionRules.fields(
        body,
        Set.of(
            "name",
            "kind",
            "location",
            "latitude",
            "longitude",
            "capability",
            "coverage_from",
            "coverage_to",
            "status",
            "status_note"));
    return repo.command(
        context,
        "coordination-source",
        key,
        body,
        () -> {
          lock(context);
          Problem.require(sources().size() < 50, 409, "CAPACITY", "Source register limit reached.");
          Instant from = CoordinationRules.instant(body, "coverage_from"),
              to = CoordinationRules.instant(body, "coverage_to");
          Problem.require(
              from.isBefore(to), 400, "VALIDATION_FAILED", "Coverage start must precede its end.");
          var source =
              Json.object(
                  "id",
                  CoordinationRules.id("CAP-"),
                  "version",
                  1,
                  "name",
                  CoordinationRules.text(body, "name", 3, 160),
                  "kind",
                  CoordinationRules.choice(
                      body, "kind", Set.of("CAMERA", "TOLL", "FIELD_REPORT", "ACCESS_LOG")),
                  "location",
                  CoordinationRules.text(body, "location", 3, 160),
                  "latitude",
                  CoordinationRules.coordinate(body, "latitude", -90, 90),
                  "longitude",
                  CoordinationRules.coordinate(body, "longitude", -180, 180),
                  "capability",
                  CoordinationRules.text(body, "capability", 5, 500),
                  "coverage_from",
                  from.toString(),
                  "coverage_to",
                  to.toString(),
                  "status",
                  CoordinationRules.choice(
                      body, "status", Set.of("AVAILABLE", "PARTIAL", "OFFLINE", "NOT_CONNECTED")),
                  "status_note",
                  CoordinationRules.text(body, "status_note", 5, 500),
                  "updated_at",
                  Instant.now().toString(),
                  "owner",
                  context.publicView().get("organisation"),
                  "synthetic",
                  true);
          db.update(
              "insert into observation_sources(id,tenant_id,version,body) values(?,?,1,?::jsonb)",
              source.get("id"),
              context.tenant(),
              Json.encode(source));
          access(context, (String) source.get("id"), "REGISTER");
          return source;
        });
  }

  Map<String, Object> status(
      Policy.Context context, String id, String key, Map<String, Object> body) {
    MissionRules.fields(body, Set.of("expected_version", "status", "status_note"));
    return repo.command(
        context,
        "source-status:" + id,
        key,
        body,
        () -> {
          lock(context);
          var source = source(id);
          CoordinationRules.version(source, body);
          source.put(
              "status",
              CoordinationRules.choice(
                  body, "status", Set.of("AVAILABLE", "PARTIAL", "OFFLINE", "NOT_CONNECTED")));
          source.put("status_note", CoordinationRules.text(body, "status_note", 5, 500));
          source.put("updated_at", Instant.now().toString());
          source.put("version", Json.number(source, "version") + 1);
          db.update(
              "update observation_sources set version=?,body=?::jsonb where id=?",
              source.get("version"),
              Json.encode(source),
              id);
          access(context, id, "STATUS_UPDATED");
          return source;
        });
  }

  Map<String, Object> ingest(Policy.Context context, String key, Map<String, Object> body) {
    MissionRules.fields(
        body,
        Set.of(
            "source_id",
            "producer_event_id",
            "occurred_at",
            "clock_uncertainty_seconds",
            "description",
            "original",
            "supersedes_id"));
    return repo.command(
        context,
        "observation",
        key,
        body,
        () -> {
          lock(context);
          String sid = CoordinationRules.text(body, "source_id", 4, 80),
              producer = CoordinationRules.text(body, "producer_event_id", 1, 120);
          var source = source(sid);
          String fingerprint = Json.hash(Json.encode(body));
          var found =
              db.queryForList(
                  "select fingerprint,body::text as body from observations where source_id=? and producer_event_id=?",
                  sid,
                  producer);
          if (!found.isEmpty()) {
            Problem.require(
                fingerprint.equals(found.getFirst().get("fingerprint")),
                409,
                "EVENT_CONFLICT",
                "This producer event already has different content. Record a correction as a new event.");
            return Json.decode((String) found.getFirst().get("body"));
          }
          Problem.require(
              db.queryForObject("select count(*) from observations", Integer.class) < 2000,
              409,
              "CAPACITY",
              "Observation register limit reached.");
          Instant at = CoordinationRules.instant(body, "occurred_at"), received = Instant.now();
          int uncertainty = CoordinationRules.integer(body, "clock_uncertainty_seconds", 0, 86400);
          String original = MissionRules.original(body);
          Problem.require(
              original.length() <= 8000,
              400,
              "VALIDATION_FAILED",
              "Observation originals are limited to 8000 characters.");
          String predecessor = null;
          if (body.containsKey("supersedes_id")) {
            predecessor = CoordinationRules.text(body, "supersedes_id", 4, 80);
            var prior = observation(predecessor);
            Problem.require(
                prior.get("source_id").equals(sid),
                400,
                "VALIDATION_FAILED",
                "A correction must refer to the same source.");
            Problem.require(
                db.queryForObject(
                        "select count(*) from observations where body->>'supersedes_id'=?",
                        Integer.class,
                        predecessor)
                    == 0,
                409,
                "CORRECTION_CONFLICT",
                "This observation already has a correction.");
          }
          var flags = new ArrayList<String>();
          if (at.plusSeconds(300).isBefore(received)) flags.add("LATE_ARRIVAL");
          if (at.isAfter(received.plusSeconds(uncertainty))) flags.add("FUTURE_SOURCE_TIME");
          if (uncertainty > 0) flags.add("CLOCK_UNCERTAINTY");
          if (!source.get("status").equals("AVAILABLE")) flags.add("SOURCE_LIMITED");
          if (predecessor != null) flags.add("CORRECTION");
          var result =
              Json.object(
                  "id",
                  CoordinationRules.id("OBS-"),
                  "source_id",
                  sid,
                  "source_name",
                  source.get("name"),
                  "source_version",
                  source.get("version"),
                  "source_kind",
                  source.get("kind"),
                  "owner",
                  source.get("owner"),
                  "producer_event_id",
                  producer,
                  "occurred_at",
                  at.toString(),
                  "received_at",
                  received.toString(),
                  "clock_uncertainty_seconds",
                  uncertainty,
                  "description",
                  CoordinationRules.text(body, "description", 5, 500),
                  "original",
                  original,
                  "sha256",
                  Json.hash(original),
                  "latitude",
                  source.get("latitude"),
                  "longitude",
                  source.get("longitude"),
                  "location",
                  source.get("location"),
                  "flags",
                  flags,
                  "supersedes_id",
                  predecessor,
                  "synthetic",
                  true);
          db.update(
              "insert into observations(id,tenant_id,source_id,producer_event_id,fingerprint,occurred_at,received_at,body) values(?,?,?,?,?,?::timestamptz,?::timestamptz,?::jsonb)",
              result.get("id"),
              context.tenant(),
              sid,
              producer,
              fingerprint,
              at.toString(),
              received.toString(),
              Json.encode(result));
          access(context, (String) result.get("id"), "INGEST");
          return result;
        });
  }

  Map<String, Object> search(Policy.Context context, Map<String, Object> body) {
    MissionRules.fields(body, Set.of("from", "to", "query", "source_id"));
    Instant from = CoordinationRules.instant(body, "from"),
        to = CoordinationRules.instant(body, "to");
    Problem.require(
        from.isBefore(to) && from.plusSeconds(31L * 86400).compareTo(to) >= 0,
        400,
        "VALIDATION_FAILED",
        "Search a positive window of at most 31 days.");
    String query =
        body.containsKey("query")
            ? CoordinationRules.text(body, "query", 0, 160).toLowerCase(Locale.ROOT)
            : "";
    String sid =
        body.containsKey("source_id") ? CoordinationRules.text(body, "source_id", 4, 80) : null;
    return repo.scoped(
        context,
        () -> {
          if (sid != null) source(sid);
          access(context, "observations", "SEARCH");
          var observations =
              db
                  .query(
                      "select body::text from observations where occurred_at+make_interval(secs => (body->>'clock_uncertainty_seconds')::int)>=?::timestamptz and occurred_at-make_interval(secs => (body->>'clock_uncertainty_seconds')::int)<=?::timestamptz order by occurred_at,id",
                      (rs, n) -> Json.decode(rs.getString(1)),
                      from.toString(),
                      to.toString())
                  .stream()
                  .filter(
                      o ->
                          (sid == null || sid.equals(o.get("source_id")))
                              && (o.get("description") + " " + o.get("original"))
                                  .toLowerCase(Locale.ROOT)
                                  .contains(query))
                  .toList();
          var coverage = new ArrayList<Map<String, Object>>();
          for (var source : sources()) {
            if (sid != null && !sid.equals(source.get("id"))) continue;
            boolean full =
                !Instant.parse((String) source.get("coverage_from")).isAfter(from)
                    && !Instant.parse((String) source.get("coverage_to")).isBefore(to);
            long matches =
                observations.stream()
                    .filter(o -> source.get("id").equals(o.get("source_id")))
                    .count();
            String state = (String) source.get("status");
            String outcome =
                !state.equals("AVAILABLE")
                    ? state
                    : !full ? "PARTIAL" : matches > 0 ? "MATCHES" : "NO_MATCH_IN_DECLARED_COVERAGE";
            coverage.add(
                Json.object(
                    "source_id",
                    source.get("id"),
                    "name",
                    source.get("name"),
                    "outcome",
                    outcome,
                    "matches",
                    matches,
                    "coverage_from",
                    source.get("coverage_from"),
                    "coverage_to",
                    source.get("coverage_to"),
                    "note",
                    source.get("status_note"),
                    "status_at",
                    source.get("updated_at")));
          }
          return Json.object(
              "observations",
              observations,
              "coverage",
              coverage,
              "from",
              from.toString(),
              "to",
              to.toString(),
              "queried_at",
              Instant.now().toString());
        });
  }

  List<Map<String, Object>> snapshot(List<String> ids) {
    return ids.stream().map(this::observation).toList();
  }
}
