// Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-27 — Role-specific actions and spatial/time projections
import { describe, it, expect } from "vitest";
import {
  mapPositions,
  orderedObservations,
  requestActions,
  type AgencyRequest,
  type Capability,
  type Observation,
} from "../src/lib/coordination";
import type { Identity } from "../src/lib/contracts";
const sender: Identity = {
  actor: "gauteng",
  name: "Synthetic sender",
  organisation: "Gauteng",
  agency_id: "gauteng-demo",
  role: "COORDINATOR",
  purpose: "INCIDENT_COORDINATION",
  read_only: false,
  synthetic: true,
};
const recipient = { ...sender, actor: "limpopo", agency_id: "limpopo-demo" };
const request: AgencyRequest = {
  id: "REQ-test",
  case_id: "CASE-test",
  sender_tenant: sender.agency_id,
  recipient_tenant: recipient.agency_id,
  title: "Synthetic review request",
  task: "Inspect the selected originals.",
  due_at: "2026-09-28T00:00:00Z",
  expires_at: "2026-09-29T00:00:00Z",
  release_reason: "A source explicitly selected for this case.",
  created_at: "2026-09-27T00:00:00Z",
  state: "RELEASED",
  version: 2,
  revoked_at: null,
  accepted_by: null,
};
const now = Date.parse("2026-09-27T12:00:00Z");
describe("agency action projection", () => {
  it("separates receipt, acceptance, work and result ownership", () => {
    expect(requestActions(request, recipient, now)).toEqual(["DELIVERED"]);
    expect(
      requestActions({ ...request, state: "DELIVERED" }, recipient, now),
    ).toEqual(["ACCEPTED", "DECLINED"]);
    expect(
      requestActions({ ...request, state: "IN_PROGRESS" }, recipient, now),
    ).toEqual(["RESULT_RECORDED", "OUTCOME_UNKNOWN"]);
    expect(
      requestActions({ ...request, state: "RESULT_RECORDED" }, sender, now),
    ).toContain("CLOSED");
    expect(
      requestActions({ ...request, state: "RESULT_RECORDED" }, recipient, now),
    ).not.toContain("CLOSED");
  });
  it("denies read-only and nonparticipant actions", () => {
    expect(
      requestActions(request, { ...recipient, read_only: true }, now),
    ).toEqual([]);
    expect(
      requestActions(request, { ...recipient, agency_id: "care-demo" }, now),
    ).toEqual([]);
  });
  it("denies recipient actions at the expiry boundary and after revocation", () => {
    expect(
      requestActions(request, recipient, Date.parse(request.expires_at)),
    ).toEqual([]);
    expect(
      requestActions(
        { ...request, revoked_at: "2026-09-27T11:00:00Z" },
        recipient,
        now,
      ),
    ).toEqual([]);
    expect(
      requestActions(
        { ...request, revoked_at: "2026-09-27T11:00:00Z" },
        sender,
        now,
      ),
    ).toContain("CANCELLED");
  });
  it("does not release an expired draft or offer acceptance to its sender", () => {
    expect(
      requestActions(
        { ...request, state: "DRAFT" },
        sender,
        Date.parse(request.expires_at),
      ),
    ).not.toContain("RELEASED");
    expect(
      requestActions({ ...request, state: "DELIVERED" }, sender, now),
    ).not.toContain("ACCEPTED");
  });
});
describe("observation projections", () => {
  it("orders by event time rather than arrival time without changing input order", () => {
    const records = [
      {
        id: "OBS-late",
        occurred_at: "2026-09-27T08:00:00Z",
        received_at: "2026-09-27T10:00:00Z",
      },
      {
        id: "OBS-first",
        occurred_at: "2026-09-27T09:00:00Z",
        received_at: "2026-09-27T09:01:00Z",
      },
    ] as Observation[];
    const reversed = [...records].reverse();
    expect(orderedObservations(reversed).map((o) => o.id)).toEqual([
      "OBS-late",
      "OBS-first",
    ]);
    expect(reversed[0].id).toBe("OBS-first");
  });
  it("keeps a single source finite and puts northern sources above southern ones", () => {
    const sources = [
      { id: "south", latitude: -26, longitude: 28 },
      { id: "north", latitude: -24, longitude: 28.5 },
    ] as Capability[];
    const points = mapPositions(sources);
    expect(points[1].y).toBeLessThan(points[0].y);
    expect(
      mapPositions([sources[0]]).every(
        (p) => Number.isFinite(p.x) && Number.isFinite(p.y),
      ),
    ).toBe(true);
    expect(mapPositions([])).toEqual([]);
  });
});
