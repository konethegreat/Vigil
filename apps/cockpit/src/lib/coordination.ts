// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Typed observation and coordination views
import type { Identity } from "./contracts";
export type Capability = {
  id: string;
  version: number;
  name: string;
  kind: string;
  location: string;
  latitude: number;
  longitude: number;
  capability: string;
  coverage_from: string;
  coverage_to: string;
  status: string;
  status_note: string;
  updated_at: string;
  owner: string;
};
export type Observation = {
  id: string;
  source_id: string;
  source_name: string;
  source_version: number;
  source_kind: string;
  owner: string;
  producer_event_id: string;
  occurred_at: string;
  received_at: string;
  clock_uncertainty_seconds: number;
  description: string;
  original: string;
  sha256: string;
  latitude: number;
  longitude: number;
  location: string;
  flags: string[];
  supersedes_id: string | null;
};
export type JointCase = {
  id: string;
  title: string;
  objective: string;
  location: string;
  occurred_at: string;
  purpose: string;
  owner_tenant: string;
  peer_tenant: string;
  created_by: string;
  created_at: string;
};
export type AgencyRequest = {
  id: string;
  case_id: string;
  sender_tenant: string;
  recipient_tenant: string;
  title: string;
  task: string;
  due_at: string;
  expires_at: string;
  release_reason: string;
  created_at: string;
  state: string;
  version: number;
  revoked_at: string | null;
  accepted_by: string | null;
  updated_at?: string;
};
export type Packet = {
  content: {
    kind: string;
    request_id: string;
    owner_tenant: string;
    recipient_tenant: string;
    expires_at: string;
    released_by: string;
    prepared_at: string;
    observations: Observation[];
  };
  fingerprint: string;
  revoked_at: string | null;
};
export type Receipt = {
  id: string;
  request_id: string;
  kind: string;
  note: string;
  actor: string;
  agency: string;
  at: string;
  previous_hash: string;
  hash: string;
};
export function receiptClockRegressed(
  receipts: Pick<Receipt, "at">[],
): boolean {
  return receipts.some(
    (receipt, index) =>
      index > 0 && Date.parse(receipt.at) < Date.parse(receipts[index - 1].at),
  );
}
export type RequestDetail = AgencyRequest & {
  packets: Packet[];
  receipts: Receipt[];
  release_active: boolean;
  audit: {
    verified: boolean;
    head_hash: string;
    event_count: number;
    scope: string;
  };
};
export type CaseDetail = {
  case: JointCase;
  requests: RequestDetail[];
  retrieved_at: string;
};
export type CoordinationWorkspace = {
  identity: Identity;
  agencies: { id: string; name: string }[];
  sources: Capability[];
  observations: Observation[];
  cases: JointCase[];
  requests: AgencyRequest[];
  server_time: string;
};
export type Coverage = {
  source_id: string;
  name: string;
  outcome: string;
  matches: number;
  coverage_from: string;
  coverage_to: string;
  note: string;
  status_at: string;
};
export type ObservationSearch = {
  observations: Observation[];
  coverage: Coverage[];
  from: string;
  to: string;
  queried_at: string;
};

export const actionLabel: Record<string, string> = {
  RELEASED: "Release to agency",
  DELIVERED: "Acknowledge receipt",
  ACCEPTED: "Accept responsibility",
  IN_PROGRESS: "Start work",
  RESULT_RECORDED: "Record result",
  CLOSED: "Close request",
  DECLINED: "Decline request",
  OUTCOME_UNKNOWN: "Record unknown outcome",
  CANCELLED: "Cancel request",
  REVOKED: "Revoke evidence access",
};
export function requestActions(
  request: AgencyRequest,
  identity: Identity,
  now = Date.now(),
): string[] {
  if (identity.read_only) return [];
  const sender = request.sender_tenant === identity.agency_id;
  if (!sender && request.recipient_tenant !== identity.agency_id) return [];
  const active =
    !request.revoked_at &&
    Date.parse(request.expires_at) > now &&
    request.state !== "CANCELLED";
  if (sender) {
    const actions: string[] = [];
    if (request.state === "DRAFT" && active) actions.push("RELEASED");
    if (["RESULT_RECORDED", "OUTCOME_UNKNOWN"].includes(request.state))
      actions.push("CLOSED");
    if (
      !["CLOSED", "CANCELLED", "DECLINED", "RESULT_RECORDED"].includes(
        request.state,
      )
    )
      actions.push("CANCELLED");
    if (request.state !== "DRAFT" && !request.revoked_at)
      actions.push("REVOKED");
    return actions;
  }
  if (!active) return [];
  return (
    (
      {
        RELEASED: ["DELIVERED"],
        DELIVERED: ["ACCEPTED", "DECLINED"],
        ACCEPTED: ["IN_PROGRESS"],
        IN_PROGRESS: ["RESULT_RECORDED", "OUTCOME_UNKNOWN"],
      } as Record<string, string[]>
    )[request.state] || []
  );
}
export function orderedObservations(records: Observation[]): Observation[] {
  return [...records].sort(
    (a, b) =>
      Date.parse(a.occurred_at) - Date.parse(b.occurred_at) ||
      a.id.localeCompare(b.id),
  );
}
export function mapPositions(sources: Capability[]) {
  if (!sources.length) return [];
  const lats = sources.map((s) => s.latitude),
    lons = sources.map((s) => s.longitude);
  const minLat = Math.min(...lats) - 0.05,
    maxLat = Math.max(...lats) + 0.05,
    minLon = Math.min(...lons) - 0.05,
    maxLon = Math.max(...lons) + 0.05;
  return sources.map((source) => ({
    source,
    x: 55 + ((source.longitude - minLon) / (maxLon - minLon)) * 470,
    y: 45 + ((maxLat - source.latitude) / (maxLat - minLat)) * 280,
  }));
}
export function isoLocal(value: string): string {
  return new Date(value).toISOString();
}
export function groupedMapPositions(sources: Capability[]) {
  const grouped = new Map<
    string,
    { sources: Capability[]; x: number; y: number }
  >();
  for (const { source, x, y } of mapPositions(sources)) {
    const key = `${source.latitude}:${source.longitude}`;
    const group = grouped.get(key) || { sources: [], x, y };
    group.sources.push(source);
    grouped.set(key, group);
  }
  return [...grouped.values()];
}
export function localInput(value = new Date()): string {
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
}
