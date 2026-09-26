// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Investigation read models and view projections
export const objectTypes = [
  "PERSON",
  "ORGANISATION",
  "PLACE",
  "ASSET",
  "EVENT",
  "DOCUMENT",
] as const;
export type ObjectType = (typeof objectTypes)[number];
export const relations = [
  "LOCATED_AT",
  "INVOLVES",
  "REPORTED_BY",
  "SUPPLIED_BY",
  "RELATED_TO",
  "PRECEDES",
] as const;
export type Assessment = "REPORTED" | "CORROBORATED" | "DISPUTED";
export type Provenance = {
  mission_title: string;
  evidence_title: string;
  evidence_status: string;
  current_evidence_version: number;
  needs_review: boolean;
};
export type SourcedRecord = {
  id: string;
  mission_id: string;
  evidence_id: string;
  evidence_version: number;
  original_sha256: string;
  created_at: string;
  created_by: string;
  provenance: Provenance;
};
export type InvestigationObject = SourcedRecord & {
  object_type: ObjectType;
  title: string;
  summary: string;
  occurred_at: string | null;
};
export type InvestigationLink = SourcedRecord & {
  from_id: string;
  to_id: string;
  relation: string;
  assessment: Assessment;
  reason: string;
  revision: number;
  history: {
    revision: number;
    assessment: Assessment;
    reason: string;
    actor: string;
    evidence_version: number;
    recorded_at: string;
  }[];
};
export type InvestigationGraph = {
  objects: InvestigationObject[];
  links: InvestigationLink[];
  object_limit: number;
  link_limit: number;
  synthetic: true;
};

export function projectGraph(
  graph: InvestigationGraph,
  missionId: string,
  query: string,
  type: string,
) {
  const missionLinks = graph.links.filter(
    (link) => !missionId || link.mission_id === missionId,
  );
  const endpoints = new Set(
    missionLinks.flatMap((link) => [link.from_id, link.to_id]),
  );
  const objects = graph.objects.filter(
    (object) =>
      (!missionId ||
        object.mission_id === missionId ||
        endpoints.has(object.id)) &&
      (!type || object.object_type === type) &&
      `${object.title} ${object.summary} ${object.id}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const ids = new Set(objects.map((object) => object.id));
  return {
    objects,
    links: missionLinks.filter(
      (link) => ids.has(link.from_id) && ids.has(link.to_id),
    ),
  };
}

export function timelineObjects(objects: InvestigationObject[]) {
  return objects
    .filter((object) => object.object_type === "EVENT")
    .sort((a, b) => {
      if (!a.occurred_at && !b.occurred_at) return a.id.localeCompare(b.id);
      if (!a.occurred_at) return 1;
      if (!b.occurred_at) return -1;
      return (
        Date.parse(a.occurred_at) - Date.parse(b.occurred_at) ||
        a.id.localeCompare(b.id)
      );
    });
}
