// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Core enforced lineage graph, Warden
// W5 marking propagation records, and decision-lineage projection (P4 task 4.6,
// VIGIL_DOMAIN_BLUEPRINT section 10)
package vigil.core.lineage;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Enforced lineage graph store (`datasets -> transforms -> objects -> Actions -> documents`),
 * Warden W5 marking-propagation record keeper, and decision-lineage explainability engine
 * (`VIGIL_DOMAIN_BLUEPRINT.md` section 10).
 */
public final class LineageGraphStore {

  public enum NodeKind {
    DATASET,
    TRANSFORM,
    OBJECT_TYPE,
    ACTION_RUN,
    DOCUMENT
  }

  public record SecurityLabel(
      List<String> markings,
      int classification,
      List<String> releasability,
      List<String> purposeTags) {
    public SecurityLabel {
      markings = List.copyOf(new TreeSet<>(Objects.requireNonNull(markings, "markings")));
      if (classification < 0 || classification > 4) {
        throw new IllegalArgumentException("classification must be in 0..4");
      }
      releasability =
          List.copyOf(new TreeSet<>(Objects.requireNonNull(releasability, "releasability")));
      purposeTags = List.copyOf(new TreeSet<>(Objects.requireNonNull(purposeTags, "purposeTags")));
      if (purposeTags.isEmpty()) {
        throw new IllegalArgumentException(
            "purposeTags must be explicitly declared (use UNRESTRICTED when empty)");
      }
    }
  }

  public record LineageNode(
      String rid,
      NodeKind kind,
      String branch,
      String versionOrSnapshot,
      SecurityLabel label,
      Instant recordedAt) {
    public LineageNode {
      if (rid == null || rid.isBlank()) {
        throw new IllegalArgumentException("rid is required");
      }
      Objects.requireNonNull(kind, "kind");
      branch = (branch == null || branch.isBlank()) ? "main" : branch;
      if (versionOrSnapshot == null || versionOrSnapshot.isBlank()) {
        throw new IllegalArgumentException("versionOrSnapshot is required");
      }
      Objects.requireNonNull(label, "label");
      recordedAt = Objects.requireNonNullElseGet(recordedAt, Instant::now);
    }
  }

  public record LineageEdge(
      String fromRid,
      String toRid,
      String transformOrRunId,
      List<String> propagatedMarkings,
      int propagatedClassification,
      Instant recordedAt) {}

  public record MarkingPropagationRecord(
      String propagationId,
      String targetRid,
      List<String> inputRids,
      SecurityLabel propagatedLabel,
      String policyVersion,
      Instant evaluatedAt) {}

  public record DecisionRecord(
      String actionRunId,
      String actionType,
      String actorPrincipal,
      String purpose,
      String caseId,
      String justificationId,
      String approvalRequestId,
      String agentSessionId,
      String modelId,
      String parameterDiffJson,
      List<String> objectRidsRead,
      List<String> datasetTransactionRids,
      String indexWatermark,
      List<String> sourceDocumentSha256s,
      String riskExplanationRef,
      String wardenPolicyVersion,
      String auditEventHash,
      long editSeqStart,
      long editSeqEnd,
      Instant recordedAt) {
    public DecisionRecord {
      if (actionRunId == null || actionRunId.isBlank()) {
        throw new IllegalArgumentException("actionRunId is required");
      }
      if (actionType == null || actionType.isBlank()) {
        throw new IllegalArgumentException("actionType is required");
      }
      if (actorPrincipal == null || actorPrincipal.isBlank()) {
        throw new IllegalArgumentException("actorPrincipal is required");
      }
      if (purpose == null || purpose.isBlank()) {
        throw new IllegalArgumentException("purpose is required");
      }
      if ((agentSessionId == null) != (modelId == null)) {
        throw new IllegalArgumentException(
            "agentSessionId and modelId must both be present or both be null");
      }
      if (approvalRequestId != null && (justificationId == null || justificationId.isBlank())) {
        throw new IllegalArgumentException("every approval requires a bound justificationId");
      }
      if (auditEventHash == null || auditEventHash.length() != 64) {
        throw new IllegalArgumentException("auditEventHash must be a 64-hex SHA-256 digest");
      }
      if (editSeqEnd < editSeqStart) {
        throw new IllegalArgumentException("editSeqEnd must be >= editSeqStart");
      }
      objectRidsRead = List.copyOf(Objects.requireNonNull(objectRidsRead, "objectRidsRead"));
      datasetTransactionRids =
          List.copyOf(Objects.requireNonNull(datasetTransactionRids, "datasetTransactionRids"));
      sourceDocumentSha256s =
          List.copyOf(Objects.requireNonNull(sourceDocumentSha256s, "sourceDocumentSha256s"));
      recordedAt = Objects.requireNonNullElseGet(recordedAt, Instant::now);
    }
  }

  public record DecisionExplanation(
      DecisionRecord decision,
      List<LineageNode> upstreamNodes,
      List<LineageEdge> upstreamEdges,
      List<MarkingPropagationRecord> propagations) {}

  private final Map<String, LineageNode> nodes = new LinkedHashMap<>();
  private final List<LineageEdge> edges = new ArrayList<>();
  private final List<MarkingPropagationRecord> propagations = new ArrayList<>();
  private final Map<String, DecisionRecord> decisions = new LinkedHashMap<>();
  private final Map<Long, String> editSeqToActionRun = new LinkedHashMap<>();

  /**
   * Computes Warden W5 label propagation across one or more input labels: markings = union,
   * classification = max, releasability = intersection of non-empty sets (fail-closed if disjoint),
   * purposeTags = intersection (with UNRESTRICTED acting as the top element; fail-closed if
   * disjoint).
   */
  public static SecurityLabel propagateLabels(List<SecurityLabel> inputs) {
    if (inputs == null || inputs.isEmpty()) {
      throw new IllegalArgumentException("propagateLabels requires at least one input label");
    }
    Set<String> markings = new TreeSet<>();
    int maxClassification = 0;
    Set<String> relIntersection = null;
    Set<String> purposeIntersection = null;

    for (SecurityLabel in : inputs) {
      markings.addAll(in.markings());
      if (in.classification() > maxClassification) {
        maxClassification = in.classification();
      }
      if (!in.releasability().isEmpty()) {
        if (relIntersection == null) {
          relIntersection = new TreeSet<>(in.releasability());
        } else {
          relIntersection.retainAll(in.releasability());
          if (relIntersection.isEmpty()) {
            throw new IllegalStateException(
                "W5 propagation failed: disjoint releasability sets across inputs");
          }
        }
      }
      List<String> inPurposes = in.purposeTags();
      if (!inPurposes.contains("UNRESTRICTED")) {
        if (purposeIntersection == null) {
          purposeIntersection = new TreeSet<>(inPurposes);
        } else {
          purposeIntersection.retainAll(inPurposes);
          if (purposeIntersection.isEmpty()) {
            throw new IllegalStateException(
                "W5 propagation failed: disjoint purposeTags across inputs");
          }
        }
      }
    }

    List<String> finalRel = relIntersection == null ? List.of() : List.copyOf(relIntersection);
    List<String> finalPurposes =
        purposeIntersection == null ? List.of("UNRESTRICTED") : List.copyOf(purposeIntersection);
    return new SecurityLabel(List.copyOf(markings), maxClassification, finalRel, finalPurposes);
  }

  public synchronized void registerNode(LineageNode node) {
    Objects.requireNonNull(node, "node");
    nodes.put(node.rid(), node);
  }

  /**
   * Records a lineage transition from upstream input nodes to a target node, enforcing Warden W5
   * non-downgrade (`target` must carry at least the propagated markings and classification of its
   * inputs).
   */
  public synchronized MarkingPropagationRecord recordLineageTransition(
      String propagationId,
      List<LineageNode> inputs,
      LineageNode target,
      String transformOrRunId,
      String policyVersion) {
    if (inputs == null || inputs.isEmpty()) {
      throw new IllegalArgumentException("inputs must not be empty");
    }
    Objects.requireNonNull(target, "target");
    List<SecurityLabel> inputLabels = inputs.stream().map(LineageNode::label).toList();
    SecurityLabel propagated = propagateLabels(inputLabels);

    if (!target.label().markings().containsAll(propagated.markings())
        || target.label().classification() < propagated.classification()) {
      throw new IllegalStateException(
          "W5 violation: target node " + target.rid() + " downgrades propagated security label");
    }

    Instant now = Instant.now();
    for (LineageNode input : inputs) {
      nodes.put(input.rid(), input);
      edges.add(
          new LineageEdge(
              input.rid(),
              target.rid(),
              transformOrRunId,
              propagated.markings(),
              propagated.classification(),
              now));
    }
    nodes.put(target.rid(), target);

    MarkingPropagationRecord record =
        new MarkingPropagationRecord(
            propagationId,
            target.rid(),
            inputs.stream().map(LineageNode::rid).toList(),
            propagated,
            policyVersion,
            now);
    propagations.add(record);
    return record;
  }

  public synchronized void recordDecision(DecisionRecord decision) {
    Objects.requireNonNull(decision, "decision");
    decisions.put(decision.actionRunId(), decision);
    for (long seq = decision.editSeqStart(); seq <= decision.editSeqEnd(); seq++) {
      editSeqToActionRun.put(seq, decision.actionRunId());
    }
  }

  public synchronized List<LineageNode> getUpstreamNodes(String targetRid, int maxDepth) {
    int limit = maxDepth <= 0 ? 10 : maxDepth;
    Set<String> visited = new LinkedHashSet<>();
    Deque<Map.Entry<String, Integer>> queue = new ArrayDeque<>();
    queue.add(Map.entry(targetRid, 0));

    List<LineageNode> result = new ArrayList<>();
    while (!queue.isEmpty()) {
      Map.Entry<String, Integer> current = queue.removeFirst();
      String rid = current.getKey();
      int depth = current.getValue();
      if (depth >= limit) {
        continue;
      }
      for (LineageEdge edge : edges) {
        if (edge.toRid().equals(rid) && visited.add(edge.fromRid())) {
          LineageNode upstream = nodes.get(edge.fromRid());
          if (upstream != null) {
            result.add(upstream);
          }
          queue.addLast(Map.entry(edge.fromRid(), depth + 1));
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  public synchronized DecisionExplanation explainDecision(String actionRunId) {
    DecisionRecord decision = decisions.get(actionRunId);
    if (decision == null) {
      throw new IllegalArgumentException("Unknown actionRunId: " + actionRunId);
    }
    Set<String> seedRids = new LinkedHashSet<>(decision.objectRidsRead());
    seedRids.addAll(decision.datasetTransactionRids());

    Map<String, LineageNode> discoveredNodes = new LinkedHashMap<>();
    List<LineageEdge> discoveredEdges = new ArrayList<>();
    List<MarkingPropagationRecord> discoveredPropagations = new ArrayList<>();

    for (String seed : seedRids) {
      LineageNode seedNode = nodes.get(seed);
      if (seedNode != null) {
        discoveredNodes.put(seedNode.rid(), seedNode);
      }
      for (LineageNode up : getUpstreamNodes(seed, 10)) {
        discoveredNodes.put(up.rid(), up);
      }
    }
    for (LineageEdge edge : edges) {
      if (discoveredNodes.containsKey(edge.toRid())
          || discoveredNodes.containsKey(edge.fromRid())) {
        discoveredEdges.add(edge);
      }
    }
    for (MarkingPropagationRecord prop : propagations) {
      if (discoveredNodes.containsKey(prop.targetRid())) {
        discoveredPropagations.add(prop);
      }
    }

    return new DecisionExplanation(
        decision,
        List.copyOf(discoveredNodes.values()),
        List.copyOf(discoveredEdges),
        List.copyOf(discoveredPropagations));
  }

  public synchronized DecisionExplanation explainEditSequence(long editSeq) {
    String actionRunId = editSeqToActionRun.get(editSeq);
    if (actionRunId == null) {
      throw new IllegalArgumentException("No decision recorded for edit_log sequence " + editSeq);
    }
    return explainDecision(actionRunId);
  }
}
