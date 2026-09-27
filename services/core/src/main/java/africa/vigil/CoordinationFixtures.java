// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Fictional corridor observations;
// no live agency connection
package africa.vigil;

import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
final class CoordinationFixtures implements ApplicationRunner {
  private final ObservationService observations;
  private final CoordinationService coordination;

  CoordinationFixtures(ObservationService observations, CoordinationService coordination) {
    this.observations = observations;
    this.coordination = coordination;
  }

  @Override
  public void run(ApplicationArguments args) {
    var gp = Policy.identity("gauteng");
    var lp = Policy.identity("limpopo");
    var south =
        source(
            gp,
            "south",
            "Southern access record",
            "ACCESS_LOG",
            "Fictional corridor · southern point",
            -25.78,
            28.29,
            "AVAILABLE",
            "Synthetic event register. Vehicle descriptions only; no driver identification.");
    var north =
        source(
            gp,
            "north",
            "Northern toll register",
            "TOLL",
            "Fictional corridor · northern point",
            -25.04,
            28.40,
            "PARTIAL",
            "The source clock may be up to three minutes fast; coverage has not been independently verified.");
    source(
        gp,
        "offline",
        "Middle camera segment",
        "CAMERA",
        "Fictional corridor · middle point",
        -25.43,
        28.34,
        "OFFLINE",
        "No footage available in this demonstration. An outage is a coverage gap, not a negative sighting.");
    var receiving =
        source(
            lp,
            "field",
            "Receiving team inspection",
            "FIELD_REPORT",
            "Fictional receiving point",
            -24.48,
            28.71,
            "AVAILABLE",
            "Manual synthetic inspection records. No live camera feed or automatic recognition.");
    event(
        gp,
        south,
        "south-001",
        "2026-09-27T06:10:00Z",
        0,
        "Grey delivery vehicle reported leaving the southern point.",
        "SYNTHETIC RECORD\n06:10 UTC: a grey delivery vehicle is reported at the southern access point. Partial marking SAMPLE-4? is uncertain. The record does not identify a driver or establish involvement in an incident.");
    event(
        gp,
        north,
        "north-001",
        "2026-09-27T07:35:00Z",
        180,
        "A similar vehicle description appears in the northern register; connection unconfirmed.",
        "SYNTHETIC REGISTER\n07:35 UTC on source clock: a grey vehicle, possible marking SAMPLE-47. Clock may be three minutes fast. Similarity alone does not establish the same vehicle. No image is supplied in this fixture.");
    event(
        gp,
        south,
        "south-002",
        "2026-09-27T06:20:00Z",
        0,
        "A separate blue maintenance vehicle was logged.",
        "SYNTHETIC RECORD\n06:20 UTC: a blue maintenance vehicle is recorded. No connection to the grey vehicle has been established.");
    event(
        lp,
        receiving,
        "receiving-001",
        "2026-09-27T09:00:00Z",
        60,
        "Receiving inspection cannot confirm that this is the same vehicle.",
        "SYNTHETIC FIELD NOTE\n09:00 UTC: a stationary grey delivery vehicle was observed at the fictional receiving point. The marking was unreadable. This observation cannot confirm the proposed connection or identify a driver.");
    coordination.createCase(
        gp,
        "seed-coordination-case-v1",
        Json.object(
            "title",
            "Reconcile the corridor observations",
            "objective",
            "Determine what the selected records establish, identify missing coverage, and obtain a documented receiving-team assessment.",
            "location",
            "Fictional Gauteng–Limpopo corridor",
            "occurred_at",
            "2026-09-27T06:00:00Z",
            "peer_tenant",
            lp.tenant(),
            "purpose",
            "A synthetic demonstration of evidence review and accountable coordination; no real incident or suspect is represented."));
  }

  private Map<String, Object> source(
      Policy.Context context,
      String key,
      String name,
      String kind,
      String location,
      double latitude,
      double longitude,
      String status,
      String note) {
    return observations.register(
        context,
        "seed-source-" + key + "-v1",
        Json.object(
            "name",
            name,
            "kind",
            kind,
            "location",
            location,
            "latitude",
            latitude,
            "longitude",
            longitude,
            "capability",
            "Synthetic structured records; originals are text, not footage.",
            "coverage_from",
            "2026-09-25T00:00:00Z",
            "coverage_to",
            "2026-10-04T00:00:00Z",
            "status",
            status,
            "status_note",
            note));
  }

  private void event(
      Policy.Context context,
      Map<String, Object> source,
      String key,
      String at,
      int uncertainty,
      String description,
      String original) {
    observations.ingest(
        context,
        "seed-event-" + key + "-v1",
        Json.object(
            "source_id",
            source.get("id"),
            "producer_event_id",
            key,
            "occurred_at",
            at,
            "clock_uncertainty_seconds",
            uncertainty,
            "description",
            description,
            "original",
            original));
  }
}
