// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - TypeScript type declarations for the Warden WebAssembly binding (P3 task 3.3)
export interface ConformanceDecisionSummary {
  readonly allow: boolean;
  readonly externalHttpStatus: number;
  readonly redactionCount: number;
  readonly obligationCount: number;
}

export interface WardenWasmExports {
  readonly memory: WebAssembly.Memory;
  warden_conformance_count(): number;
  warden_run_conformance_suite(): number;
  warden_eval_conformance_case(caseIndex: number): number;
}

export function decodePackedDecision(packed: number): ConformanceDecisionSummary;
export function loadWardenWasm(wasmPath?: string): Promise<WardenWasmExports>;
