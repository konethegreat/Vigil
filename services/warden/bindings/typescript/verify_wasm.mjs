// Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - TypeScript/WebAssembly binding and 551-case conformance runner for the Warden Security Kernel (P3 task 3.3)
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export function decodePackedDecision(packed) {
  const unsigned = packed >>> 0;
  if ((unsigned & 0x8000_0000) !== 0) {
    throw new RangeError("conformance case index out of bounds");
  }
  return {
    allow: (unsigned & 1) === 1,
    externalHttpStatus: (unsigned >>> 1) & 0x7fff,
    redactionCount: (unsigned >>> 16) & 0xff,
    obligationCount: (unsigned >>> 24) & 0x7f,
  };
}

export async function loadWardenWasm(wasmPath) {
  const here = dirname(fileURLToPath(import.meta.url));
  const resolved =
    wasmPath ??
    resolve(here, "../../target/wasm32-unknown-unknown/release/vigil_warden.wasm");
  const bytes = readFileSync(resolved);
  const { instance } = await WebAssembly.instantiate(bytes, {});
  return instance.exports;
}

async function main() {
  const exports = await loadWardenWasm();
  const total = exports.warden_conformance_count() >>> 0;
  if (total < 500) {
    throw new Error(`expected >= 500 conformance cases, got ${total}`);
  }
  const verified = exports.warden_run_conformance_suite() >>> 0;
  if (verified !== total) {
    throw new Error(`warden_run_conformance_suite returned ${verified}, expected ${total}`);
  }
  for (let idx = 0; idx < total; idx += 1) {
    const summary = decodePackedDecision(exports.warden_eval_conformance_case(idx));
    if (
      summary.externalHttpStatus !== 200 &&
      summary.externalHttpStatus !== 403 &&
      summary.externalHttpStatus !== 404
    ) {
      throw new Error(`unexpected HTTP status at case ${idx}: ${summary.externalHttpStatus}`);
    }
    if (summary.obligationCount < 1) {
      throw new Error(`case ${idx} missing mandatory W12 AUDIT_READ obligation`);
    }
  }
  console.log(
    `PASS [typescript-wasm-binding]: verified ${verified}/${total} Warden conformance cases + W12 audit chain`
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
