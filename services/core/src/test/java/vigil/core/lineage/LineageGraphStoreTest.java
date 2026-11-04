// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Unit tests for Core enforced lineage
// graph, W5 marking propagation, and decision-lineage explainability (P4 task 4.6)
package vigil.core.lineage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import vigil.core.lineage.LineageGraphStore.DecisionExplanation;
import vigil.core.lineage.LineageGraphStore.DecisionRecord;
import vigil.core.lineage.LineageGraphStore.LineageNode;
import vigil.core.lineage.LineageGraphStore.MarkingPropagationRecord;
import vigil.core.lineage.LineageGraphStore.NodeKind;
import vigil.core.lineage.LineageGraphStore.SecurityLabel;

class LineageGraphStoreTest {

  @Test
  void propagatesMarkingsAndExplainsDecisionEndToEnd() {
    LineageGraphStore store = new LineageGraphStore();

    SecurityLabel docLabel =
        new SecurityLabel(
            List.of("PII"), 2, List.of("REL-ZA", "REL-SADC"), List.of("PROCUREMENT_INTEGRITY"));
    SecurityLabel ocdsLabel =
        new SecurityLabel(
            List.of("FIN"),
            1,
            List.of("REL-ZA"),
            List.of("PROCUREMENT_INTEGRITY", "DUE_DILIGENCE"));

    LineageNode docNode =
        new LineageNode(
            "doc:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            NodeKind.DOCUMENT,
            "main",
            "v1",
            docLabel,
            Instant.parse("2026-11-04T08:00:00Z"));
    LineageNode rawDataset =
        new LineageNode(
            "ri.forge.demo.dataset.restrictions-raw",
            NodeKind.DATASET,
            "main",
            "snap-100",
            docLabel,
            Instant.parse("2026-11-04T08:05:00Z"));
    LineageNode ocdsDataset =
        new LineageNode(
            "ri.forge.demo.dataset.ocds-releases",
            NodeKind.DATASET,
            "main",
            "snap-200",
            ocdsLabel,
            Instant.parse("2026-11-04T08:06:00Z"));

    store.recordLineageTransition(
        "prop-1", List.of(docNode), rawDataset, "docai-extract-01", "warden@1.0.0");

    SecurityLabel derivedLabel = LineageGraphStore.propagateLabels(List.of(docLabel, ocdsLabel));
    assertEquals(List.of("FIN", "PII"), derivedLabel.markings());
    assertEquals(2, derivedLabel.classification());
    assertEquals(List.of("REL-ZA"), derivedLabel.releasability());
    assertEquals(List.of("PROCUREMENT_INTEGRITY"), derivedLabel.purposeTags());

    LineageNode supplierObject =
        new LineageNode(
            "ri.ontology.demo.object.supplier-001",
            NodeKind.OBJECT_TYPE,
            "main",
            "ver-3",
            derivedLabel,
            Instant.parse("2026-11-04T08:10:00Z"));

    MarkingPropagationRecord prop2 =
        store.recordLineageTransition(
            "prop-2",
            List.of(rawDataset, ocdsDataset),
            supplierObject,
            "transform-supplier-match",
            "warden@1.0.0");
    assertEquals(List.of("FIN", "PII"), prop2.propagatedLabel().markings());

    DecisionRecord decision =
        new DecisionRecord(
            "01926e62-1111-7000-8000-000000000001",
            "refer-supplier@1",
            "user:analyst-za-1",
            "PROCUREMENT_INTEGRITY",
            "case-2026-09",
            "jst-001",
            "apr-001",
            "agent-sess-01",
            "qwen3-32b@1",
            "{\"threshold_override\":false}",
            List.of("ri.ontology.demo.object.supplier-001"),
            List.of("ri.forge.demo.dataset.restrictions-raw"),
            "wm-1042",
            List.of("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"),
            "risk-exp-001",
            "warden@1.0.0",
            "a".repeat(64),
            500L,
            502L,
            Instant.parse("2026-11-04T08:15:00Z"));
    store.recordDecision(decision);

    DecisionExplanation explanation = store.explainEditSequence(501L);
    assertEquals("refer-supplier@1", explanation.decision().actionType());
    assertEquals(4, explanation.upstreamNodes().size());
    assertEquals(3, explanation.upstreamEdges().size());
    assertEquals(2, explanation.propagations().size());
  }

  @Test
  void rejectsDowngradedLabelsAndInvalidDecisions() {
    LineageGraphStore store = new LineageGraphStore();
    SecurityLabel piiLabel =
        new SecurityLabel(List.of("PII"), 2, List.of(), List.of("UNRESTRICTED"));
    SecurityLabel publicLabel = new SecurityLabel(List.of(), 0, List.of(), List.of("UNRESTRICTED"));

    LineageNode input =
        new LineageNode("ri.forge.demo.dataset.in", NodeKind.DATASET, "", "s1", piiLabel, null);
    LineageNode downgraded =
        new LineageNode(
            "ri.forge.demo.dataset.out", NodeKind.DATASET, "main", "s2", publicLabel, null);

    store.registerNode(input);
    assertThrows(
        IllegalStateException.class,
        () -> store.recordLineageTransition("p-bad", List.of(input), downgraded, "run-1", "v1"));

    SecurityLabel relZa =
        new SecurityLabel(List.of(), 1, List.of("REL-ZA"), List.of("PROCUREMENT_INTEGRITY"));
    SecurityLabel relKe =
        new SecurityLabel(List.of(), 1, List.of("REL-KE"), List.of("PROCUREMENT_INTEGRITY"));
    assertThrows(
        IllegalStateException.class,
        () -> LineageGraphStore.propagateLabels(List.of(relZa, relKe)));

    SecurityLabel purpA =
        new SecurityLabel(List.of(), 1, List.of(), List.of("PROCUREMENT_INTEGRITY"));
    SecurityLabel purpB =
        new SecurityLabel(List.of(), 1, List.of(), List.of("CORRIDOR_OPERATIONS"));
    assertThrows(
        IllegalStateException.class,
        () -> LineageGraphStore.propagateLabels(List.of(purpA, purpB)));

    assertThrows(
        IllegalArgumentException.class,
        () -> new SecurityLabel(List.of(), 5, List.of(), List.of("UNRESTRICTED")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SecurityLabel(List.of(), 0, List.of(), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LineageNode("", NodeKind.DATASET, "main", "s1", publicLabel, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new LineageNode("rid-1", NodeKind.DATASET, "main", "", publicLabel, null));
    assertThrows(
        IllegalArgumentException.class, () -> LineageGraphStore.propagateLabels(List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> store.recordLineageTransition("p-empty", List.of(), downgraded, "run", "v1"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "",
                "act",
                "user:1",
                "PURP",
                null,
                null,
                null,
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "a".repeat(64),
                1L,
                1L,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "run-1",
                "",
                "user:1",
                "PURP",
                null,
                null,
                null,
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "a".repeat(64),
                1L,
                1L,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "run-1",
                "act",
                "",
                "PURP",
                null,
                null,
                null,
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "a".repeat(64),
                1L,
                1L,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "run-1",
                "act",
                "user:1",
                "",
                null,
                null,
                null,
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "a".repeat(64),
                1L,
                1L,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "run-1",
                "act",
                "user:1",
                "PURP",
                null,
                null,
                null,
                "sess-only",
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "a".repeat(64),
                1L,
                1L,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "run-1",
                "act",
                "user:1",
                "PURP",
                null,
                null,
                "approval-without-justification",
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "a".repeat(64),
                1L,
                1L,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "run-1",
                "act",
                "user:1",
                "PURP",
                null,
                "jst-1",
                "apr-1",
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "short-hash",
                1L,
                1L,
                null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DecisionRecord(
                "run-1",
                "act",
                "user:1",
                "PURP",
                null,
                "jst-1",
                "apr-1",
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                "wm",
                List.of(),
                null,
                "v1",
                "a".repeat(64),
                5L,
                2L,
                null));

    assertThrows(IllegalArgumentException.class, () -> store.explainDecision("missing"));
    assertThrows(IllegalArgumentException.class, () -> store.explainEditSequence(999L));
    assertTrue(store.getUpstreamNodes("ri.forge.demo.dataset.in", 0).isEmpty());
  }
}
