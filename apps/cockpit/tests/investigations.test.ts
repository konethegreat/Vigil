// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Cross-case projections and event chronology
import { expect, it } from "vitest";
import {
  projectGraph,
  timelineObjects,
  type InvestigationGraph,
  type InvestigationObject,
  type InvestigationLink,
} from "../src/lib/investigations";

const object = (
  id: string,
  mission: string,
  title: string,
  at: string | null = null,
): InvestigationObject => ({
  id,
  mission_id: mission,
  title,
  summary: "Synthetic source-backed object",
  object_type: "EVENT",
  occurred_at: at,
  evidence_id: "source-1",
  evidence_version: 1,
  original_sha256: "a".repeat(64),
  created_at: "2026-09-26T10:00:00Z",
  created_by: "Test",
  provenance: {
    mission_title: mission,
    evidence_title: "Source",
    evidence_status: "UNVERIFIED",
    current_evidence_version: 1,
    needs_review: false,
  },
});
const objects = [
  object("A", "case-1", "Depot event"),
  object("B", "case-2", "Receiving event"),
  object("C", "case-3", "Unrelated event"),
];
const link: InvestigationLink = {
  ...objects[0],
  id: "link-1",
  mission_id: "case-2",
  from_id: "A",
  to_id: "B",
  relation: "RELATED_TO",
  assessment: "DISPUTED",
  reason: "Conflicting sources",
  revision: 1,
  history: [],
};
const graph: InvestigationGraph = {
  objects,
  links: [link],
  object_limit: 500,
  link_limit: 1000,
  synthetic: true,
};

it("case selection includes referenced objects from other cases without unrelated records", () => {
  const result = projectGraph(graph, "case-2", "", "");
  expect(result.objects.map((o) => o.id)).toEqual(["A", "B"]);
  expect(result.links).toEqual([link]);
  expect(result.objects[0].mission_id).toBe("case-1");
});
it("search and type filters never leave edges pointing to hidden endpoints", () => {
  expect(projectGraph(graph, "", "DEPOT", "")).toEqual({
    objects: [objects[0]],
    links: [],
  });
  expect(projectGraph(graph, "", "", "ASSET")).toEqual({
    objects: [],
    links: [],
  });
});
it("timeline uses occurrence time across offsets and leaves unknown times explicit", () => {
  const late = object("late", "case-1", "Late", "2026-09-26T10:00:00+02:00");
  const early = object("early", "case-1", "Early", "2026-09-26T07:30:00Z");
  const input = [
    objects[0],
    late,
    { ...objects[1], object_type: "DOCUMENT" as const },
    early,
  ];
  expect(timelineObjects(input).map((o) => o.id)).toEqual([
    "early",
    "late",
    "A",
  ]);
  expect(input[0]).toBe(objects[0]);
  expect(timelineObjects(input)[2].occurred_at).toBeNull();
});
