// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Explicitly synthetic
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
// operational fixtures
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation task semantics and
// seed order
package africa.vigil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
final class DemoFixtures implements ApplicationRunner {
  private final MissionRepository repository;

  DemoFixtures(MissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public void run(ApplicationArguments args) {
    var ops = Policy.identity("operations");
    var care = Policy.identity("protection");
    var corridor =
        mission(
            "VGL-0041",
            "Restore the corridor. Preserve the evidence.",
            "LIFELINES",
            "HIGH",
            "Gauteng · North–South corridor",
            "Restore communications along the affected corridor, reconcile conflicting incident times, and confirm service with the receiving operator.",
            ops.name());
    corridor.put("subtitle", "Corridor communications interruption");
    add(
        corridor,
        evidence(
            "Field inspection · sector 04",
            "Synthetic field team report",
            "FIELD_REPORT",
            "The field team reports damage to a communications cable in sector 04. The cause has not been established.",
            "SYNTHETIC TRAINING RECORD\n06:42 SAST — Field team observed a damaged cable enclosure in sector 04. No attribution of cause. Inspection photographs are not included in this fixture.",
            "SUPPORTED"));
    add(
        corridor,
        evidence(
            "Network event log",
            "Synthetic telemetry adapter",
            "SENSOR",
            "The network log places the first loss of signal at 05:58 SAST, earlier than the inspection record.",
            "SYNTHETIC TRAINING RECORD\n05:58 SAST — Sector 04 link unavailable.\n06:03 SAST — Sector 07 link unavailable.\nSource clock accuracy has not been independently checked.",
            "DISPUTED"));
    add(
        corridor,
        evidence(
            "Repair crew availability",
            "Synthetic maintenance coordinator",
            "SERVICE_UPDATE",
            "A repair crew is available; cable stock still needs confirmation.",
            "SYNTHETIC TRAINING RECORD\nCrew R-02 available for assessment. Stock allocation pending. This is not a dispatch receipt.",
            "UNVERIFIED"));
    repository.seed(ops, corridor);
    var water =
        mission(
            "VGL-0042",
            "Reconnect a community to water.",
            "LIFELINES",
            "CRITICAL",
            "Gauteng · Mamelodi",
            "Confirm the pump fault, allocate the appropriate repair team, and verify restoration with the affected facilities.",
            ops.name());
    water.put("subtitle", "Pump station service interruption");
    add(
        water,
        evidence(
            "Pump controller alert",
            "Synthetic water operations feed",
            "SENSOR",
            "The pump controller reports an electrical fault; field confirmation is outstanding.",
            "SYNTHETIC TRAINING RECORD\nPump P-12 fault: supply interruption. Impact and root cause require on-site confirmation.",
            "UNVERIFIED"));
    repository.seed(ops, water);
    var flood =
        mission(
            "VGL-0043",
            "Make every assistance request count.",
            "RESPONSE",
            "HIGH",
            "KwaZulu-Natal · eThekwini",
            "Reconcile incoming flood-related assistance requests, confirm available resources, and track accepted handoffs through completion.",
            ops.name());
    flood.put("subtitle", "Community flood assistance coordination");
    add(
        flood,
        evidence(
            "Community liaison update",
            "Synthetic coordination desk",
            "FIELD_REPORT",
            "Three assistance requests await a confirmed receiving organisation.",
            "SYNTHETIC TRAINING RECORD\nThree unassigned assistance requests. Locations and requirements must be confirmed before allocation. No real emergency calls are represented.",
            "UNVERIFIED"));
    repository.seed(ops, flood);
    var protect =
        mission(
            "VGL-P018",
            "A supported path, from first contact.",
            "PROTECT",
            "HIGH",
            "Protected location",
            "Coordinate the assistance requested by protected case C-018, respecting agreed contact preferences and confirming each support handoff.",
            care.name());
    protect.put("subtitle", "Confidential support coordination");
    add(
        protect,
        evidence(
            "Agreed contact preferences",
            "Synthetic practitioner intake",
            "STATEMENT",
            "The person requested contact through the assigned practitioner only. No automated outbound contact is authorised.",
            "SYNTHETIC TRAINING RECORD\nCase C-018. Contact via assigned practitioner only. Do not send notifications or share a location. No personal identifiers or real case data.",
            "SUPPORTED"));
    add(
        protect,
        evidence(
            "Provider capacity request",
            "Synthetic support provider",
            "SERVICE_UPDATE",
            "A support provider has received a capacity enquiry but has not accepted a referral.",
            "SYNTHETIC TRAINING RECORD\nCapacity enquiry received. Referral acceptance remains pending. Sending an enquiry does not confirm delivery of care.",
            "UNVERIFIED"));
    repository.seed(care, protect);
    var followup =
        mission(
            "VGL-P019",
            "Keep the next handoff from being lost.",
            "PROTECT",
            "STANDARD",
            "Protected location",
            "Confirm the agreed follow-up pathway for protected case C-019 and record the practitioner's verified outcome.",
            care.name());
    followup.put("subtitle", "Continuity of support");
    add(
        followup,
        evidence(
            "Follow-up request",
            "Synthetic practitioner record",
            "STATEMENT",
            "The case owner needs confirmation from the receiving provider before arranging the next step.",
            "SYNTHETIC TRAINING RECORD\nC-019 follow-up. No direct contact details. Practitioner-mediated coordination only.",
            "SUPPORTED"));
    repository.seed(care, followup);
  }

  static Map<String, Object> mission(
      String id,
      String title,
      String domain,
      String priority,
      String location,
      String objective,
      String owner) {
    String now = Instant.now().toString();
    boolean protection = domain.equals("PROTECT");
    var tasks = new ArrayList<Map<String, Object>>();
    tasks.add(
        task(
            protection
                ? "Confirm the agreed support pathway"
                : "Validate the incident and resource requirements",
            protection ? "Assigned practitioner" : "Operations desk"));
    tasks.add(
        task(
            protection
                ? "Obtain a receiving provider's acknowledgment"
                : "Obtain a receiving team's acknowledgment",
            protection ? "Support provider" : "Field coordination"));
    tasks.add(
        task(
            protection
                ? "Verify the agreed support was delivered"
                : "Verify restoration and record the outcome",
            protection ? "Case owner" : "Mission owner"));
    if (domain.equals("INVESTIGATE") || domain.equals("INTEGRITY")) {
      tasks.clear();
      tasks.add(task("Preserve and reconcile the source material", "Evidence custodian"));
      tasks.add(
          task("Review relationship claims and alternative explanations", "Investigation analyst"));
      tasks.add(task("Verify findings and document unresolved questions", "Case reviewer"));
    }
    return Json.object(
        "id",
        id,
        "title",
        title,
        "subtitle",
        domain.equals("PROTECT") ? "Protection mission" : "Operational mission",
        "domain",
        domain,
        "priority",
        priority,
        "status",
        "ACTIVE",
        "location",
        location,
        "objective",
        objective,
        "owner",
        owner,
        "created_at",
        now,
        "updated_at",
        now,
        "version",
        1,
        "evidence_version",
        0,
        "evidence",
        new ArrayList<Map<String, Object>>(),
        "tasks",
        tasks,
        "plan",
        null,
        "outcome",
        null,
        "synthetic",
        true);
  }

  static Map<String, Object> evidence(
      String title, String source, String kind, String statement, String original, String status) {
    return Json.object(
        "id",
        "SRC-" + UUID.randomUUID().toString().substring(0, 8),
        "title",
        title,
        "source",
        source,
        "kind",
        kind,
        "statement",
        statement,
        "original",
        original,
        "status",
        status,
        "observed_at",
        Instant.now().minus(40, ChronoUnit.MINUTES).toString(),
        "recorded_at",
        Instant.now().toString(),
        "sha256",
        Json.hash(original),
        "history",
        new ArrayList<Map<String, Object>>());
  }

  private static void add(Map<String, Object> mission, Map<String, Object> evidence) {
    Json.rows(mission.get("evidence")).add(evidence);
    mission.put("evidence_version", Json.number(mission, "evidence_version") + 1);
  }

  private static Map<String, Object> task(String title, String owner) {
    return Json.object(
        "id",
        "TSK-" + UUID.randomUUID().toString().substring(0, 8),
        "title",
        title,
        "owner",
        owner,
        "state",
        "PROPOSED",
        "receipt",
        null,
        "recorded_by",
        null,
        "updated_at",
        Instant.now().toString());
  }
}
