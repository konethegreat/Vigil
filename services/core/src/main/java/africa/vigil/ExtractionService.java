// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Atomic source review and
// publication
package africa.vigil;

import static africa.vigil.MissionRules.text;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
final class ExtractionService {
  private final MissionRepository missions;
  private final InvestigationService graph;
  private final MindClient mind;
  private final JdbcTemplate db;

  ExtractionService(
      MissionRepository missions, InvestigationService graph, MindClient mind, JdbcTemplate db) {
    this.missions = missions;
    this.graph = graph;
    this.mind = mind;
    this.db = db;
  }

  Map<String, Object> list(Policy.Context context, String missionId) {
    return missions.scoped(
        context,
        () -> {
          var mission = missions.get(context, missionId);
          var runs =
              db.query(
                  "select body::text,decision::text from extraction_runs where mission_id=? order by created_at desc,id",
                  (rs, row) -> {
                    var run = Json.decode(rs.getString(1));
                    run.put(
                        "decision", rs.getString(2) == null ? null : Json.decode(rs.getString(2)));
                    run.put(
                        "stale",
                        !run.get("evidence_version").equals(mission.get("evidence_version")));
                    return run;
                  },
                  missionId);
          return Json.object("runs", runs);
        });
  }

  Map<String, Object> propose(Policy.Context context, String key, Map<String, Object> request) {
    Policy.requireWrite(context);
    MissionRules.fields(request, Set.of("mission_id", "evidence_id", "expected_version"));
    String missionId = text(request, "mission_id", 3, 60),
        sourceId = text(request, "evidence_id", 3, 60);
    String scope = "extraction:" + missionId;
    var replay = missions.replay(context, scope, key, request);
    if (replay != null) return replay;
    var snapshot = missions.get(context, missionId);
    writable(snapshot, request);
    var source = InvestigationService.source(snapshot, sourceId);
    missions.scoped(
        context,
        () -> {
          available(missionId, sourceId, snapshot);
          return null;
        });
    // No database locks or transaction are held while a model is running.
    var response =
        ExtractionRules.validate(
            mind.extract(context, snapshot, source), (String) source.get("original"));
    return missions.command(
        context,
        scope,
        key,
        request,
        () -> {
          var mission = missions.read(missionId, true);
          writable(mission, request);
          Problem.require(
              mission.get("evidence_version").equals(snapshot.get("evidence_version"))
                  && InvestigationService.source(mission, sourceId)
                      .get("sha256")
                      .equals(source.get("sha256")),
              409,
              "SOURCE_CHANGED",
              "The evidence changed during extraction. Run it again against the current source.");
          available(missionId, sourceId, mission);
          String id = "EXT-" + UUID.randomUUID();
          var run =
              Json.object(
                  "id",
                  id,
                  "mission_id",
                  missionId,
                  "evidence_id",
                  sourceId,
                  "evidence_version",
                  mission.get("evidence_version"),
                  "original_sha256",
                  source.get("sha256"),
                  "created_at",
                  Instant.now().toString(),
                  "created_by",
                  context.name());
          run.putAll(response);
          db.update(
              "insert into extraction_runs(tenant_id,id,mission_id,evidence_id,evidence_version,original_sha256,body) values(?,?,?,?,?,?,?::jsonb)",
              context.tenant(),
              id,
              missionId,
              sourceId,
              mission.get("evidence_version"),
              source.get("sha256"),
              Json.encode(run));
          bump(mission);
          missions.append(
              context,
              missionId,
              "SOURCE_EXTRACTED",
              Json.encode(
                  Json.object(
                      "run_id",
                      id,
                      "source_sha256",
                      source.get("sha256"),
                      "proposal_sha256",
                      Json.hash(Json.encode(run)),
                      "model",
                      response.get("model"))));
          run.put("decision", null);
          run.put("stale", false);
          return Json.object("run", run, "mission", mission);
        });
  }

  private void available(String mission, String evidence, Map<String, Object> snapshot) {
    Problem.require(
        db.queryForObject(
                "select count(*) from extraction_runs where mission_id=? and evidence_id=? and evidence_version=?",
                Integer.class,
                mission,
                evidence,
                snapshot.get("evidence_version"))
            == 0,
        409,
        "EXTRACTION_EXISTS",
        "A proposal already exists for this source snapshot. Open its review.");
    Problem.require(
        db.queryForObject(
                "select count(*) from extraction_runs where mission_id=?", Integer.class, mission)
            < 100,
        409,
        "EXTRACTION_LIMIT",
        "This local mission has reached its limit of 100 extraction runs.");
  }

  Map<String, Object> review(
      Policy.Context context, String id, String key, Map<String, Object> request) {
    Policy.requireWrite(context);
    MissionRules.fields(request, Set.of("expected_version", "object_ids", "link_ids", "note"));
    String note = text(request, "note", 5, 1000);
    return missions.command(
        context,
        "extraction-review:" + id,
        key,
        request,
        () -> {
          // Match the registry's lock order before acquiring the mission lock.
          graph.graphLock(context);
          var rows =
              db.queryForList(
                  "select body::text,decision::text from extraction_runs where id=? for update",
                  id);
          Problem.require(
              !rows.isEmpty(),
              404,
              "NOT_FOUND",
              "The extraction is unavailable in this workspace.");
          var row = rows.getFirst();
          Problem.require(
              row.get("decision") == null,
              409,
              "ALREADY_REVIEWED",
              "This extraction already has a final decision.");
          var run = Json.decode((String) row.get("body"));
          String missionId = (String) run.get("mission_id");
          var mission = missions.read(missionId, true);
          writable(mission, request);
          var source = InvestigationService.source(mission, (String) run.get("evidence_id"));
          Problem.require(
              run.get("evidence_version").equals(mission.get("evidence_version"))
                  && run.get("original_sha256").equals(source.get("sha256")),
              409,
              "SOURCE_CHANGED",
              "The evidence changed. Extract from the current snapshot before publishing.");
          var proposal = Json.map(run.get("proposal"));
          var objects = Json.rows(proposal.get("objects"));
          var links = Json.rows(proposal.get("links"));
          var selectedObjects = ExtractionRules.selected(request, "object_ids", objects);
          var selectedLinks = ExtractionRules.selected(request, "link_ids", links);
          for (var link : links)
            if (selectedLinks.contains(link.get("id")))
              Problem.require(
                  selectedObjects.contains(link.get("from_id"))
                      && selectedObjects.contains(link.get("to_id")),
                  400,
                  "MISSING_ENDPOINT",
                  "Selected relationships require both proposed objects to be selected.");
          var objectRecords = Json.object();
          var linkRecords = Json.object();
          for (var object : objects)
            if (selectedObjects.contains(object.get("id"))) {
              var command =
                  Json.object(
                      "object_type",
                      object.get("object_type"),
                      "title",
                      object.get("title"),
                      "summary",
                      object.get("summary"),
                      "evidence_id",
                      run.get("evidence_id"));
              objectRecords.put(
                  (String) object.get("id"), graph.createObject(context, mission, command));
            }
          for (var link : links)
            if (selectedLinks.contains(link.get("id"))) {
              var command =
                  Json.object(
                      "from_id",
                      objectRecords.get(link.get("from_id")),
                      "to_id",
                      objectRecords.get(link.get("to_id")),
                      "relation",
                      link.get("relation"),
                      "evidence_id",
                      run.get("evidence_id"),
                      "assessment",
                      "REPORTED",
                      "reason",
                      link.get("reason") + " Review: " + note);
              linkRecords.put((String) link.get("id"), graph.createLink(context, mission, command));
            }
          var decision =
              Json.object(
                  "object_ids",
                  selectedObjects.stream().sorted().toList(),
                  "link_ids",
                  selectedLinks.stream().sorted().toList(),
                  "object_records",
                  objectRecords,
                  "link_records",
                  linkRecords,
                  "note",
                  note,
                  "actor",
                  context.name(),
                  "reviewed_at",
                  Instant.now().toString());
          db.update(
              "update extraction_runs set decision=?::jsonb where id=?", Json.encode(decision), id);
          bump(mission);
          missions.append(
              context,
              missionId,
              "EXTRACTION_REVIEWED",
              Json.encode(Json.object("run_id", id, "decision", decision)));
          run.put("decision", decision);
          run.put("stale", false);
          return Json.object("run", run, "mission", mission);
        });
  }

  private void bump(Map<String, Object> mission) {
    mission.put("version", Json.number(mission, "version") + 1);
    mission.put("updated_at", Instant.now().toString());
    missions.save(mission);
  }

  private static void writable(Map<String, Object> mission, Map<String, Object> request) {
    MissionRules.expectedVersion(mission, request);
    Problem.require(
        !mission.get("status").equals("COMPLETED"),
        409,
        "MISSION_COMPLETED",
        "Completed missions are preserved.");
  }
}
