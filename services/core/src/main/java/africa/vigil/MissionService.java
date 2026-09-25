// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Evidence-to-outcome mission
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// operations
package africa.vigil;

import static africa.vigil.MissionRules.text;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
final class MissionService {
  private final MissionRepository repository;
  private final MindClient mind;

  MissionService(MissionRepository repository, MindClient mind) {
    this.repository = repository;
    this.mind = mind;
  }

  Map<String, Object> create(Policy.Context context, String key, Map<String, Object> request) {
    MissionRules.fields(request, Set.of("domain", "title", "objective", "location", "priority"));
    String domain = text(request, "domain", 3, 24);
    Policy.requireDomain(context, domain);
    return repository.command(
        context,
        "create",
        key,
        request,
        () -> {
          String id = "VGL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
          String title = text(request, "title", 5, 160),
              objective = text(request, "objective", 10, 2000),
              location = text(request, "location", 2, 100);
          String priority = text(request, "priority", 3, 20);
          Problem.require(
              Set.of("CRITICAL", "HIGH", "STANDARD").contains(priority),
              400,
              "VALIDATION_FAILED",
              "Select a valid priority.");
          var mission =
              DemoFixtures.mission(
                  id, title, domain, priority, location, objective, context.name());
          repository.insert(context, mission);
          repository.append(context, id, "MISSION_CREATED", "Mission opened: " + title);
          return mission;
        });
  }

  Map<String, Object> apply(
      Policy.Context context, String id, String key, Map<String, Object> request) {
    Policy.requireWrite(context);
    String operation = text(request, "type", 3, 40);
    var allowed = new java.util.HashSet<>(Set.of("type", "expected_version"));
    allowed.addAll(
        switch (operation) {
          case "add_evidence" -> Set.of("title", "source", "kind", "statement", "original");
          case "review_evidence" -> Set.of("evidence_id", "status", "note", "statement");
          case "transition_task" -> Set.of("task_id", "state", "receipt");
          case "record_outcome" -> Set.of("summary", "verification");
          case "generate_plan" -> Set.<String>of();
          default -> throw new Problem(400, "UNKNOWN_COMMAND", "Unknown mission operation.");
        });
    MissionRules.fields(request, allowed);
    var replay = repository.replay(context, id, key, request);
    if (replay != null) return replay;
    Map<String, Object> proposedPlan = null;
    if (operation.equals("generate_plan")) {
      var snapshot = repository.get(context, id);
      MissionRules.expectedVersion(snapshot, request);
      Problem.require(
          !snapshot.get("status").equals("COMPLETED"),
          409,
          "MISSION_COMPLETED",
          "Completed missions are preserved as outcome records.");
      Problem.require(
          !Json.rows(snapshot.get("evidence")).isEmpty(),
          409,
          "EVIDENCE_REQUIRED",
          "Add at least one source before generating a plan.");
      proposedPlan = mind.plan(context, snapshot);
    }
    final Map<String, Object> plan = proposedPlan;
    return repository.command(
        context,
        id,
        key,
        request,
        () -> {
          var mission = repository.read(id, true);
          MissionRules.expectedVersion(mission, request);
          Problem.require(
              !mission.get("status").equals("COMPLETED"),
              409,
              "MISSION_COMPLETED",
              "Completed missions are preserved as outcome records.");
          String detail;
          switch (operation) {
            case "add_evidence" -> {
              Problem.require(
                  Json.rows(mission.get("evidence")).size() < 40,
                  409,
                  "SOURCE_LIMIT",
                  "This mission supports up to 40 source records.");
              var source =
                  DemoFixtures.evidence(
                      text(request, "title", 3, 160),
                      text(request, "source", 2, 160),
                      text(request, "kind", 3, 30),
                      text(request, "statement", 5, 2000),
                      text(request, "original", 5, 16000),
                      "UNVERIFIED");
              source.put("observed_at", null);
              Problem.require(
                  Set.of("FIELD_REPORT", "DOCUMENT", "SENSOR", "SERVICE_UPDATE", "STATEMENT")
                      .contains(source.get("kind")),
                  400,
                  "VALIDATION_FAILED",
                  "Select a valid source type.");
              Json.rows(mission.get("evidence")).add(source);
              mission.put("evidence_version", Json.number(mission, "evidence_version") + 1);
              detail = "Source added: " + source.get("title");
            }
            case "review_evidence" -> {
              var evidence =
                  find(Json.rows(mission.get("evidence")), text(request, "evidence_id", 3, 60));
              String status = text(request, "status", 3, 30),
                  note = text(request, "note", 5, 1000),
                  statement = text(request, "statement", 5, 2000);
              Problem.require(
                  Set.of("SUPPORTED", "DISPUTED", "UNVERIFIED").contains(status),
                  400,
                  "VALIDATION_FAILED",
                  "Select a valid evidence assessment.");
              Json.rows(evidence.get("history"))
                  .add(
                      Json.object(
                          "statement",
                          evidence.get("statement"),
                          "status",
                          evidence.get("status"),
                          "changed_at",
                          Instant.now().toString(),
                          "actor",
                          context.name(),
                          "reason",
                          note));
              evidence.put("statement", statement);
              evidence.put("status", status);
              mission.put("evidence_version", Json.number(mission, "evidence_version") + 1);
              detail = "Source assessment changed: " + evidence.get("title") + ". " + note;
            }
            case "generate_plan" -> {
              Problem.require(
                  plan != null
                      && mission.get("evidence_version").equals(plan.get("evidence_version")),
                  409,
                  "PLAN_STALE",
                  "Evidence changed during analysis. Generate a new plan.");
              validatePlanCitations(mission, plan);
              mission.put("plan", plan);
              detail =
                  "Plan prepared against evidence revision "
                      + mission.get("evidence_version")
                      + " using "
                      + plan.get("mode")
                      + ".";
            }
            case "transition_task" -> {
              var task = find(Json.rows(mission.get("tasks")), text(request, "task_id", 3, 60));
              String target = text(request, "state", 3, 30),
                  receipt = text(request, "receipt", 5, 1000);
              MissionRules.transition(
                  (String) task.get("state"), target, MissionRules.currentPlan(mission));
              task.put("state", target);
              task.put("receipt", receipt);
              task.put("updated_at", Instant.now().toString());
              task.put("recorded_by", context.name());
              detail = task.get("title") + " → " + target + ". " + receipt;
            }
            case "record_outcome" -> {
              Problem.require(
                  MissionRules.currentPlan(mission),
                  409,
                  "PLAN_STALE",
                  "Refresh the plan against the current evidence before closing this mission.");
              Problem.require(
                  Json.rows(mission.get("tasks")).stream()
                      .allMatch(task -> task.get("state").equals("CONFIRMED")),
                  409,
                  "TASKS_INCOMPLETE",
                  "Every task needs a confirmed result before the mission can close.");
              mission.put(
                  "outcome",
                  Json.object(
                      "summary",
                      text(request, "summary", 10, 2000),
                      "verification",
                      text(request, "verification", 10, 2000),
                      "recorded_by",
                      context.name(),
                      "recorded_at",
                      Instant.now().toString()));
              mission.put("status", "COMPLETED");
              detail = "Outcome recorded and mission completed.";
            }
            default -> throw new Problem(400, "UNKNOWN_COMMAND", "Unknown mission operation.");
          }
          mission.put("version", Json.number(mission, "version") + 1);
          mission.put("updated_at", Instant.now().toString());
          repository.save(mission);
          repository.append(context, id, operation.toUpperCase(), detail);
          return mission;
        });
  }

  private void validatePlanCitations(Map<String, Object> mission, Map<String, Object> plan) {
    Set<String> sourceIds =
        Json.rows(mission.get("evidence")).stream()
            .map(e -> (String) e.get("id"))
            .collect(java.util.stream.Collectors.toSet());
    for (var hypothesis : Json.rows(plan.get("hypotheses"))) {
      Object raw = hypothesis.get("evidence_ids");
      Problem.require(
          raw instanceof List<?> && ((List<?>) raw).stream().allMatch(sourceIds::contains),
          502,
          "INVALID_MODEL_OUTPUT",
          "The analysis referenced an unavailable source.");
    }
  }

  private static Map<String, Object> find(List<Map<String, Object>> items, String id) {
    return items.stream()
        .filter(item -> id.equals(item.get("id")))
        .findFirst()
        .orElseThrow(
            () ->
                new Problem(
                    404, "NOT_FOUND", "The selected record is unavailable in this mission."));
  }
}
