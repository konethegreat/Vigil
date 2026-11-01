// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Example library operation (P1 task 1.4)
/**
 * An example operation that consumes, but never builds, policy context.
 *
 * Only the tenant and purpose are attached to the span; record values stay out
 * of telemetry (BACKEND_GUIDE §9.2, B5).
 */
import { trace } from "@opentelemetry/api";
import type { Tracer } from "@opentelemetry/api";

import { requirePolicyContext } from "./policy.js";
import type { PolicyContext } from "./policy.js";
import type { LibrarySettings } from "./settings.js";

const defaultTracer = trace.getTracer("vigil.library");

export interface Summary {
  readonly namespace: string;
  readonly count: number;
  readonly truncated: boolean;
}

export interface SummariseOptions {
  readonly ctx: PolicyContext | null | undefined;
  readonly settings: LibrarySettings;
  readonly tracer?: Tracer;
}

export function summarise(
  records: readonly string[],
  options: SummariseOptions,
): Summary {
  const context = requirePolicyContext(options.ctx);
  const tracer = options.tracer ?? defaultTracer;

  return tracer.startActiveSpan("vigil.library.summarise", (span) => {
    try {
      span.setAttribute("vigil.tenant", context.tenantId);
      span.setAttribute("vigil.purpose", context.purpose);

      const cleaned = records
        .map((record) => record.trim())
        .filter((record) => record.length > 0);

      return {
        namespace: options.settings.namespace,
        count: cleaned.length,
        truncated: cleaned.length > options.settings.batchSize,
      };
    } finally {
      span.end();
    }
  });
}
