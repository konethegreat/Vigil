// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Fail-closed Warden stub tests (TDD)
import { describe, expect, it } from "vitest";

import {
  PolicyDenied,
  PolicyUnavailable,
  UnavailableWarden,
} from "../src/index.js";
import type { WardenBinding } from "../src/index.js";

describe("UnavailableWarden", () => {
  const warden: WardenBinding = new UnavailableWarden();

  it("is never ready", () => {
    expect(warden.ready()).toBe(false);
  });

  it("refuses every request instead of allowing it", () => {
    expect(() =>
      warden.authorize({
        tenantId: "tenant-a",
        purpose: "claims-triage",
        token: "token",
      }),
    ).toThrow(PolicyUnavailable);
  });

  it("keeps the error types distinguishable", () => {
    expect(new PolicyUnavailable("x")).not.toBeInstanceOf(PolicyDenied);
    expect(new PolicyDenied("x")).not.toBeInstanceOf(PolicyUnavailable);
  });
});
