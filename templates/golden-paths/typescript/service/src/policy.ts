// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Fail-closed Warden binding stub (P1 task 1.4)
/**
 * The real binding arrives with P3. Until then this stub refuses every request,
 * which is the correct fail-closed behaviour (INSTRUCTIONS §9.1, B4). Replace
 * it with the P3 binding; never add an allow-all path.
 */

export interface PolicyContext {
  readonly tenantId: string;
  readonly purpose: string;
  readonly signedToken: string;
}

export interface AuthorizeRequest {
  readonly tenantId: string;
  readonly purpose: string;
  readonly token: string;
}

export class PolicyUnavailable extends Error {
  constructor(message = "policy could not be evaluated") {
    super(message);
    this.name = "PolicyUnavailable";
  }
}

export class PolicyDenied extends Error {
  constructor(message = "policy denied the request") {
    super(message);
    this.name = "PolicyDenied";
  }
}

export interface WardenBinding {
  /** Return true only when policy evaluation is available. */
  ready(): boolean;

  /** Evaluate policy and return a context, or throw. */
  authorize(request: AuthorizeRequest): PolicyContext;
}

export class UnavailableWarden implements WardenBinding {
  ready(): boolean {
    return false;
  }

  authorize(): PolicyContext {
    throw new PolicyUnavailable("the Warden binding is not wired yet");
  }
}
