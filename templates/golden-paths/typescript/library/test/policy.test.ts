// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy context contract tests (TDD)
import { describe, expect, it } from "vitest";

import { MissingPolicyContext, requirePolicyContext } from "../src/index.js";

describe("requirePolicyContext", () => {
  it("returns the caller's context unchanged", () => {
    const ctx = { tenantId: "tenant-a", purpose: "claims-triage" };

    expect(requirePolicyContext(ctx)).toBe(ctx);
  });

  it("fails closed when the caller passes no context", () => {
    expect(() => requirePolicyContext(undefined)).toThrow(MissingPolicyContext);
    expect(() => requirePolicyContext(null)).toThrow(/PolicyContext/);
  });

  it("rejects a context without a tenant", () => {
    expect(() =>
      requirePolicyContext({ tenantId: "  ", purpose: "claims-triage" }),
    ).toThrow(MissingPolicyContext);
  });

  it("rejects a context without a purpose", () => {
    expect(() =>
      requirePolicyContext({ tenantId: "tenant-a", purpose: "" }),
    ).toThrow(MissingPolicyContext);
  });
});
