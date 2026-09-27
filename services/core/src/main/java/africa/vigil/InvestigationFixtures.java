// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Connected synthetic investigation
// scenarios
package africa.vigil;

import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(20)
final class InvestigationFixtures implements ApplicationRunner {
  private final MissionRepository missions;
  private final InvestigationService investigations;
  private final JdbcTemplate db;
  private static final Policy.Context OPS = Policy.identity("operations");

  InvestigationFixtures(
      MissionRepository missions, InvestigationService investigations, JdbcTemplate db) {
    this.missions = missions;
    this.investigations = investigations;
    this.db = db;
  }

  @Override
  public void run(ApplicationArguments args) {
    missions.scoped(
        OPS,
        () -> {
          db.queryForList(
              "select pg_advisory_xact_lock(hashtextextended('vigil-investigation-fixtures-v1',0))");
          if (db.queryForObject(
                  "select count(*) from investigation_objects where mission_id='VGL-I001'",
                  Integer.class)
              > 0) return null;
          var crime =
              DemoFixtures.mission(
                  "VGL-I001",
                  "Trace the missing infrastructure components.",
                  "INVESTIGATE",
                  "HIGH",
                  "Gauteng · fictional Eastbank depot",
                  "Reconcile a reported component loss, access records and a delivery discrepancy. Establish what the records support and preserve alternative explanations.",
                  "Investigation desk");
          add(
              crime,
              "Depot discrepancy report",
              "Fictional depot inventory",
              "FIELD_REPORT",
              "A stock reconciliation reports twelve controller units missing from the Eastbank depot; loss, movement and count error remain possible.",
              "SYNTHETIC RECORD\n2026-09-24T06:20:00+02:00: inventory comparison shows twelve units fewer than the prior register. No perpetrator identified. Independent recount is pending.",
              "UNVERIFIED");
          add(
              crime,
              "Gate event extract",
              "Fictional access controller",
              "SENSOR",
              "The access system recorded an authorised service entry at 05:40 SAST. It does not establish who handled stock or explain the discrepancy.",
              "SYNTHETIC RECORD\n2026-09-24T05:40:00+02:00: maintenance access credential accepted. No vehicle, face or personal identifier is included. Clock calibration requires review.",
              "SUPPORTED");
          add(
              crime,
              "Delivery note comparison",
              "Fictional receiving register",
              "DOCUMENT",
              "A delivery note from Metsi Components lists controller units, but its quantity conflicts with the receiving register.",
              "SYNTHETIC RECORD\nDelivery note D-204 lists 24 controllers. Receiving register lists 12. Metsi Components is a fictional supplier. This discrepancy is not proof of theft or supplier involvement.",
              "DISPUTED");
          missions.seed(OPS, crime);

          var integrity =
              DemoFixtures.mission(
                  "VGL-I002",
                  "Reconcile the procurement and receiving record.",
                  "INTEGRITY",
                  "STANDARD",
                  "Gauteng · fictional service district",
                  "Verify the purchasing and receiving trail for controller units and resolve documentary discrepancies before recording findings.",
                  "Integrity analyst");
          add(
              integrity,
              "Procurement ledger extract",
              "Fictional procurement register",
              "DOCUMENT",
              "A purchase entry names Metsi Components for controller units. The ledger does not establish delivery quantity or criminal conduct.",
              "SYNTHETIC RECORD\nPurchase P-804: controller units ordered from Metsi Components. Delivery note reference D-204. Receiving quantity requires independent confirmation.",
              "SUPPORTED");
          missions.seed(OPS, integrity);
          String depot =
              object(
                  "VGL-I001",
                  0,
                  "PLACE",
                  "Eastbank service depot",
                  "Fictional storage and maintenance site named in the discrepancy report.",
                  null);
          String controllers =
              object(
                  "VGL-I001",
                  0,
                  "ASSET",
                  "Controller unit stock",
                  "The reported stock discrepancy concerns controller units; the physical count is unverified.",
                  null);
          String loss =
              object(
                  "VGL-I001",
                  0,
                  "EVENT",
                  "Stock discrepancy recorded",
                  "An inventory comparison recorded a discrepancy. Cause and responsibility are unresolved.",
                  "2026-09-24T06:20:00+02:00");
          String entry =
              object(
                  "VGL-I001",
                  1,
                  "EVENT",
                  "Service access recorded",
                  "An authorised service access event appears in the controller log. Temporal proximity does not establish causation.",
                  "2026-09-24T05:40:00+02:00");
          String supplier =
              object(
                  "VGL-I001",
                  2,
                  "ORGANISATION",
                  "Metsi Components · fictional",
                  "The supplier named in a disputed delivery document. No allegation of culpability is established.",
                  null);
          String note =
              object(
                  "VGL-I001",
                  2,
                  "DOCUMENT",
                  "Delivery note D-204",
                  "Documentary quantity differs from the receiving register.",
                  null);
          String ledger =
              object(
                  "VGL-I002",
                  0,
                  "DOCUMENT",
                  "Procurement entry P-804",
                  "A separate purchasing record references the same fictional supplier and delivery note.",
                  null);
          link(
              "VGL-I001",
              0,
              controllers,
              depot,
              "LOCATED_AT",
              "REPORTED",
              "The discrepancy report identifies this depot; independent stock verification is pending.");
          link(
              "VGL-I001",
              0,
              loss,
              controllers,
              "INVOLVES",
              "REPORTED",
              "This event concerns the controller stock; its cause has not been established.");
          link(
              "VGL-I001",
              1,
              entry,
              depot,
              "LOCATED_AT",
              "CORROBORATED",
              "The log supports the location of the access event, subject to clock review.");
          link(
              "VGL-I001",
              1,
              entry,
              loss,
              "PRECEDES",
              "REPORTED",
              "Reported timestamps order these events. Clock calibration is unresolved and sequence does not establish causation.");
          link(
              "VGL-I001",
              2,
              controllers,
              supplier,
              "SUPPLIED_BY",
              "DISPUTED",
              "The delivery record names a supplier but quantity and receipt remain disputed.");
          link(
              "VGL-I001",
              2,
              note,
              controllers,
              "RELATED_TO",
              "DISPUTED",
              "The source documents conflicting quantities; the discrepancy needs reconciliation.");
          link(
              "VGL-I002",
              0,
              ledger,
              supplier,
              "RELATED_TO",
              "REPORTED",
              "The procurement ledger names this supplier. This is an administrative relationship, not a finding of wrongdoing.");
          link(
              "VGL-I002",
              0,
              ledger,
              note,
              "RELATED_TO",
              "REPORTED",
              "The purchasing record cites the delivery note; independently verify its contents.");
          return null;
        });
  }

  private static void add(
      Map<String, Object> mission,
      String title,
      String source,
      String kind,
      String statement,
      String original,
      String status) {
    var evidence = DemoFixtures.evidence(title, source, kind, statement, original, status);
    evidence.put("observed_at", null);
    Json.rows(mission.get("evidence")).add(evidence);
    mission.put("evidence_version", Json.number(mission, "evidence_version") + 1);
  }

  private String object(
      String mission, int sourceIndex, String type, String title, String summary, String at) {
    var snapshot = missions.read(mission, false);
    return (String)
        investigations
            .command(
                OPS,
                UUID.randomUUID().toString(),
                Json.object(
                    "type",
                    "create_object",
                    "mission_id",
                    mission,
                    "expected_version",
                    snapshot.get("version"),
                    "object_type",
                    type,
                    "title",
                    title,
                    "summary",
                    summary,
                    "occurred_at",
                    at,
                    "evidence_id",
                    Json.rows(snapshot.get("evidence")).get(sourceIndex).get("id")))
            .get("record_id");
  }

  private void link(
      String mission,
      int sourceIndex,
      String from,
      String to,
      String relation,
      String assessment,
      String reason) {
    var snapshot = missions.read(mission, false);
    investigations.command(
        OPS,
        UUID.randomUUID().toString(),
        Json.object(
            "type",
            "create_link",
            "mission_id",
            mission,
            "expected_version",
            snapshot.get("version"),
            "from_id",
            from,
            "to_id",
            to,
            "relation",
            relation,
            "assessment",
            assessment,
            "reason",
            reason,
            "evidence_id",
            Json.rows(snapshot.get("evidence")).get(sourceIndex).get("id")));
  }
}
