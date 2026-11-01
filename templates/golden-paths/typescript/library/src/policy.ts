// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy context contract for libraries (P1 task 1.4)
/**
 * The caller obtains a context from Warden and passes it explicitly. A library
 * never builds one, never reads ambient state and never allows a data-bearing
 * call without one (INSTRUCTIONS §9.1).
 */

export interface PolicyContext {
  readonly tenantId: string;
  readonly purpose: string;
  readonly signedToken?: string;
}

export class MissingPolicyContext extends Error {
  constructor(message = "a PolicyContext is required for data-bearing work") {
    super(message);
    this.name = "MissingPolicyContext";
  }
}

export function requirePolicyContext(
  ctx: PolicyContext | null | undefined,
): PolicyContext {
  if (ctx === null || ctx === undefined) {
    throw new MissingPolicyContext();
  }
  if (ctx.tenantId.trim().length === 0 || ctx.purpose.trim().length === 0) {
    throw new MissingPolicyContext(
      "the PolicyContext must carry a tenant and purpose",
    );
  }
  return ctx;
}
