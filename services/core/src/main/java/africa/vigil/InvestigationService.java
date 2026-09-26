// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Source-backed investigation
// commands
package africa.vigil;

import static africa.vigil.MissionRules.text;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
final class InvestigationService {
  private final MissionRepository missions;
  private final JdbcTemplate db;
  static final Set<String> TYPES =
      Set.of("PERSON", "ORGANISATION", "PLACE", "ASSET", "EVENT", "DOCUMENT");
  static final Set<String> ASSESSMENTS = Set.of("REPORTED", "CORROBORATED", "DISPUTED");

  InvestigationService(MissionRepository missions, JdbcTemplate db) {
    this.missions = missions;
    this.db = db;
  }

  Map<String, Object> graph(Policy.Context context) {
    return missions.scoped(
        context,
        () -> {
          // Serialize registry reads with registry commands; source freshness is evaluated on read.
          graphLock(context);
          var objects = records("investigation_objects");
          var links = records("investigation_links");
          var reviews =
              db.query(
                  "select row_to_json(r)::text from investigation_link_reviews r order by recorded_at,revision",
                  (rs, row) -> {
                    var review = Json.decode(rs.getString(1));
                    review.remove("tenant_id");
                    return review;
                  });
          for (var link : links)
            link.put(
                "history",
                reviews.stream().filter(r -> r.get("link_id").equals(link.get("id"))).toList());
          db.update(
              "insert into mission_access(tenant_id,actor,purpose,operation,resource) values(?,?,?,?,?)",
              context.tenant(),
              context.actor(),
              context.purpose(),
              "GRAPH_READ",
              "investigations");
          return Json.object(
              "objects",
              objects,
              "links",
              links,
              "object_limit",
              500,
              "link_limit",
              1000,
              "synthetic",
              true);
        });
  }

  private void graphLock(Policy.Context context) {
    db.queryForList(
        "select pg_advisory_xact_lock(hashtextextended(?,0))",
        context.tenant() + ":investigation-registry");
  }

  private List<Map<String, Object>> records(String table) {
    // Table names originate only in graph(), never from a request.
    return db.query(
        "select row_to_json(o)::text,m.body::text from "
            + table
            + " o join missions m on m.id=o.mission_id and m.tenant_id=o.tenant_id order by o.created_at,o.id",
        (rs, row) -> {
          var record = Json.decode(rs.getString(1));
          var mission = Json.decode(rs.getString(2));
          var evidence = source(mission, (String) record.get("evidence_id"));
          record.remove("tenant_id");
          record.put(
              "provenance",
              Json.object(
                  "mission_title",
                  mission.get("title"),
                  "evidence_title",
                  evidence.get("title"),
                  "evidence_status",
                  evidence.get("status"),
                  "current_evidence_version",
                  mission.get("evidence_version"),
                  "needs_review",
                  !record.get("evidence_version").equals(mission.get("evidence_version"))));
          return record;
        });
  }

  Map<String, Object> command(Policy.Context context, String key, Map<String, Object> request) {
    Policy.requireWrite(context);
    String operation = text(request, "type", 3, 40), missionId = text(request, "mission_id", 3, 60);
    var allowed = new java.util.HashSet<>(Set.of("type", "mission_id", "expected_version"));
    allowed.addAll(
        switch (operation) {
          case "create_object" ->
              Set.of("object_type", "title", "summary", "occurred_at", "evidence_id");
          case "create_link" ->
              Set.of("from_id", "to_id", "relation", "evidence_id", "assessment", "reason");
          case "review_link" -> Set.of("link_id", "expected_revision", "assessment", "reason");
          default -> throw new Problem(400, "UNKNOWN_COMMAND", "Unknown investigation operation.");
        });
    MissionRules.fields(request, allowed);
    return missions.command(
        context,
        "investigations:" + missionId,
        key,
        request,
        () -> {
          graphLock(context);
          var mission = missions.read(missionId, true);
          MissionRules.expectedVersion(mission, request);
          Problem.require(
              !mission.get("status").equals("COMPLETED"),
              409,
              "MISSION_COMPLETED",
              "Completed missions are preserved.");
          String recordId;
          switch (operation) {
            case "create_object" -> recordId = createObject(context, mission, request);
            case "create_link" -> recordId = createLink(context, mission, request);
            case "review_link" -> recordId = reviewLink(context, mission, request);
            default -> throw new IllegalStateException();
          }
          mission.put("version", Json.number(mission, "version") + 1);
          mission.put("updated_at", Instant.now().toString());
          missions.save(mission);
          // Include the validated assertion content in the immutable, hashed decision record.
          missions.append(
              context,
              missionId,
              operation.toUpperCase(),
              Json.encode(Json.object("record_id", recordId, "request", request)));
          return Json.object("record_id", recordId, "mission", mission);
        });
  }

  private String createObject(
      Policy.Context context, Map<String, Object> mission, Map<String, Object> request) {
    capacity("investigation_objects", 500);
    String type = text(request, "object_type", 3, 30);
    Problem.require(
        TYPES.contains(type), 400, "VALIDATION_FAILED", "Select a supported object type.");
    String at = null;
    if (request.get("occurred_at") != null && !request.get("occurred_at").equals("")) {
      Problem.require(
          type.equals("EVENT"), 400, "VALIDATION_FAILED", "Only events have an occurrence time.");
      try {
        at = OffsetDateTime.parse(text(request, "occurred_at", 10, 50)).toInstant().toString();
      } catch (DateTimeParseException error) {
        throw new Problem(
            400, "VALIDATION_FAILED", "Use an ISO timestamp with an explicit timezone.");
      }
    }
    var evidence = source(mission, text(request, "evidence_id", 3, 60));
    String id = "OBJ-" + UUID.randomUUID();
    db.update(
        "insert into investigation_objects(tenant_id,id,object_type,title,summary,occurred_at,mission_id,evidence_id,evidence_version,original_sha256,created_by) values(?,?,?,?,?,?::timestamptz,?,?,?,?,?)",
        context.tenant(),
        id,
        type,
        text(request, "title", 3, 160),
        text(request, "summary", 5, 2000),
        at,
        mission.get("id"),
        evidence.get("id"),
        mission.get("evidence_version"),
        evidence.get("sha256"),
        context.name());
    return id;
  }

  private String createLink(
      Policy.Context context, Map<String, Object> mission, Map<String, Object> request) {
    capacity("investigation_links", 1000);
    var from = object(text(request, "from_id", 3, 60));
    var to = object(text(request, "to_id", 3, 60));
    Problem.require(
        !from.get("id").equals(to.get("id")),
        400,
        "VALIDATION_FAILED",
        "Choose two distinct objects.");
    String relation = text(request, "relation", 3, 30);
    Problem.require(
        validPair(relation, (String) from.get("object_type"), (String) to.get("object_type")),
        400,
        "INVALID_RELATION",
        "This relationship does not support the selected object types.");
    var evidence = source(mission, text(request, "evidence_id", 3, 60));
    String assessment = assessment(request), reason = text(request, "reason", 5, 2000);
    var duplicate =
        db.queryForObject(
            "select count(*) from investigation_links where mission_id=? and from_id=? and to_id=? and relation=? and evidence_id=?",
            Integer.class,
            mission.get("id"),
            from.get("id"),
            to.get("id"),
            relation,
            evidence.get("id"));
    Problem.require(
        duplicate == 0,
        409,
        "LINK_EXISTS",
        "That source already supports a recorded assertion. Review the existing relationship.");
    String id = "LNK-" + UUID.randomUUID();
    db.update(
        "insert into investigation_links(tenant_id,id,from_id,to_id,relation,mission_id,evidence_id,evidence_version,original_sha256,assessment,reason,created_by) values(?,?,?,?,?,?,?,?,?,?,?,?)",
        context.tenant(),
        id,
        from.get("id"),
        to.get("id"),
        relation,
        mission.get("id"),
        evidence.get("id"),
        mission.get("evidence_version"),
        evidence.get("sha256"),
        assessment,
        reason,
        context.name());
    reviewReceipt(context, id, 1, assessment, reason, Json.number(mission, "evidence_version"));
    return id;
  }

  private String reviewLink(
      Policy.Context context, Map<String, Object> mission, Map<String, Object> request) {
    String id = text(request, "link_id", 3, 60);
    var rows =
        db.queryForList(
            "select * from investigation_links where id=? and mission_id=? for update",
            id,
            mission.get("id"));
    Problem.require(
        !rows.isEmpty(), 404, "NOT_FOUND", "The assertion is unavailable in this mission.");
    var link = rows.getFirst();
    Problem.require(
        link.get("revision").equals(request.get("expected_revision")),
        409,
        "VERSION_CONFLICT",
        "The assertion changed. Refresh before reviewing.");
    // Re-establish source and endpoint visibility before changing an assertion.
    source(mission, (String) link.get("evidence_id"));
    object((String) link.get("from_id"));
    object((String) link.get("to_id"));
    String assessment = assessment(request), reason = text(request, "reason", 5, 2000);
    int revision = (Integer) link.get("revision") + 1;
    db.update(
        "update investigation_links set assessment=?,reason=?,revision=?,evidence_version=? where id=?",
        assessment,
        reason,
        revision,
        mission.get("evidence_version"),
        id);
    reviewReceipt(
        context, id, revision, assessment, reason, Json.number(mission, "evidence_version"));
    return id;
  }

  private void reviewReceipt(
      Policy.Context context,
      String id,
      int revision,
      String assessment,
      String reason,
      int evidenceVersion) {
    db.update(
        "insert into investigation_link_reviews(tenant_id,link_id,revision,assessment,reason,evidence_version,actor) values(?,?,?,?,?,?,?)",
        context.tenant(),
        id,
        revision,
        assessment,
        reason,
        evidenceVersion,
        context.name());
  }

  private Map<String, Object> object(String id) {
    var rows = db.queryForList("select id,object_type from investigation_objects where id=?", id);
    Problem.require(
        !rows.isEmpty(), 404, "NOT_FOUND", "An object is unavailable in this workspace.");
    return rows.getFirst();
  }

  static Map<String, Object> source(Map<String, Object> mission, String id) {
    return Json.rows(mission.get("evidence")).stream()
        .filter(e -> e.get("id").equals(id))
        .findFirst()
        .orElseThrow(
            () -> new Problem(404, "NOT_FOUND", "The source is unavailable in this mission."));
  }

  private String assessment(Map<String, Object> request) {
    String value = text(request, "assessment", 3, 30);
    Problem.require(
        ASSESSMENTS.contains(value), 400, "VALIDATION_FAILED", "Select a supported assessment.");
    return value;
  }

  private void capacity(String table, int limit) {
    Problem.require(
        db.queryForObject("select count(*) from " + table, Integer.class) < limit,
        409,
        "REGISTRY_LIMIT",
        "This local registry has reached its documented capacity.");
  }

  static boolean validPair(String relation, String from, String to) {
    return switch (relation) {
      case "LOCATED_AT" ->
          Set.of("ASSET", "EVENT", "ORGANISATION").contains(from) && to.equals("PLACE");
      case "INVOLVES" -> from.equals("EVENT") && !to.equals("EVENT");
      case "REPORTED_BY" ->
          Set.of("EVENT", "DOCUMENT").contains(from)
              && Set.of("PERSON", "ORGANISATION").contains(to);
      case "SUPPLIED_BY" -> Set.of("ASSET", "DOCUMENT").contains(from) && to.equals("ORGANISATION");
      case "PRECEDES" -> from.equals("EVENT") && to.equals("EVENT");
      case "RELATED_TO" -> TYPES.contains(from) && TYPES.contains(to);
      default -> false;
    };
  }
}
