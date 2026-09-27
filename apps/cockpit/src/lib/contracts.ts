// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Typed mission read models
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
export type EvidenceStatus = "SUPPORTED" | "DISPUTED" | "UNVERIFIED";
export type TaskState =
  | "PROPOSED"
  | "AUTHORISED"
  | "SENT"
  | "ACCEPTED"
  | "IN_PROGRESS"
  | "CONFIRMED"
  | "DECLINED"
  | "OUTCOME_UNKNOWN";
export type Evidence = {
  id: string;
  title: string;
  source: string;
  kind: string;
  statement: string;
  original: string;
  status: EvidenceStatus;
  observed_at: string | null;
  recorded_at: string;
  sha256: string;
  history: {
    statement: string;
    status: EvidenceStatus;
    changed_at: string;
    actor: string;
    reason: string;
  }[];
};
export type Task = {
  id: string;
  title: string;
  owner: string;
  state: TaskState;
  receipt: string | null;
  recorded_by: string | null;
  updated_at: string;
};
export type Plan = {
  id: string;
  generated_at: string;
  evidence_version: number;
  mode: string;
  model: string | null;
  summary: string;
  hypotheses: {
    title: string;
    assessment: string;
    explanation: string;
    evidence_ids: string[];
  }[];
  gaps: string[];
  next_steps: string[];
  warnings: string[];
  review: { state: string; concerns: string[]; model?: string };
  trace: { role: string; status: string; detail: string }[];
  citation_check: string;
  support_check: string;
};
export type Mission = {
  id: string;
  title: string;
  subtitle: string;
  domain: string;
  priority: string;
  status: string;
  location: string;
  objective: string;
  owner: string;
  created_at: string;
  updated_at: string;
  version: number;
  evidence_version: number;
  evidence: Evidence[];
  tasks: Task[];
  plan: Plan | null;
  outcome: {
    summary: string;
    verification: string;
    recorded_by: string;
    recorded_at: string;
  } | null;
  synthetic: true;
};
export type Identity = {
  actor: string;
  name: string;
  organisation: string;
  role: string;
  purpose: string;
  read_only: boolean;
  synthetic: true;
};
export type Audit = {
  verified: boolean;
  event_count: number;
  head_hash: string;
  verification_scope: string;
  events: {
    event_id: string;
    recorded_at: string;
    actor: string;
    kind: string;
    detail: string;
    previous_hash: string;
    event_hash: string;
  }[];
};
export type Intelligence = {
  mode: string;
  state?: string;
  reasoner_model: string | null;
  challenger_model: string | null;
  local_endpoint_reachable: boolean;
  challenger_ready: boolean;
  message: string;
};
export const nextStates: Record<TaskState, TaskState[]> = {
  PROPOSED: ["AUTHORISED"],
  AUTHORISED: ["SENT"],
  SENT: ["ACCEPTED", "DECLINED", "OUTCOME_UNKNOWN"],
  ACCEPTED: ["IN_PROGRESS"],
  IN_PROGRESS: ["CONFIRMED", "OUTCOME_UNKNOWN"],
  CONFIRMED: [],
  DECLINED: ["PROPOSED"],
  OUTCOME_UNKNOWN: ["ACCEPTED", "DECLINED"],
};
export const stateLabels: Record<TaskState, string> = {
  PROPOSED: "Proposed",
  AUTHORISED: "Authorised",
  SENT: "Sent",
  ACCEPTED: "Accepted",
  IN_PROGRESS: "In progress",
  CONFIRMED: "Confirmed",
  DECLINED: "Declined",
  OUTCOME_UNKNOWN: "Outcome unknown",
};
export const actionLabels: Record<TaskState, string> = {
  PROPOSED: "Reopen proposal",
  AUTHORISED: "Authorise",
  SENT: "Record send receipt",
  ACCEPTED: "Record acceptance",
  IN_PROGRESS: "Start work",
  CONFIRMED: "Confirm result",
  DECLINED: "Record decline",
  OUTCOME_UNKNOWN: "Record unknown outcome",
};

export type MissionTab =
  "Situation" | "Evidence" | "Options" | "Operations" | "Outcome";
