// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Summarise operation tests (TDD)
import {
  InMemorySpanExporter,
  SimpleSpanProcessor,
  TracerProvider,
} from "@opentelemetry/sdk-trace";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { librarySettings, summarise } from "../src/index.js";

describe("summarise", () => {
  const exporter = new InMemorySpanExporter();
  const provider = new TracerProvider({
    spanProcessors: [new SimpleSpanProcessor({ exporter })],
  });
  const tracer = provider.getTracer("vigil.library.test");
  const settings = librarySettings({ namespace: "claims", batchSize: 2 });

  beforeEach(() => {
    exporter.reset();
  });

  afterEach(() => {
    exporter.reset();
  });

  it("counts non-empty trimmed records", () => {
    const summary = summarise([" a ", "", "b", "   "], {
      ctx: { tenantId: "tenant-a", purpose: "claims-triage" },
      settings,
      tracer,
    });

    expect(summary).toEqual({
      namespace: "claims",
      count: 2,
      truncated: false,
    });
  });

  it("flags a batch that exceeds the configured size", () => {
    const summary = summarise(["a", "b", "c"], {
      ctx: { tenantId: "tenant-a", purpose: "claims-triage" },
      settings,
      tracer,
    });

    expect(summary.truncated).toBe(true);
  });

  it("records tenant and purpose on a span and nothing else", () => {
    summarise(["record-body"], {
      ctx: { tenantId: "tenant-a", purpose: "claims-triage" },
      settings,
      tracer,
    });

    const [span] = exporter.getFinishedSpans();
    expect(span?.name).toBe("vigil.library.summarise");
    expect(span?.attributes).toEqual({
      "vigil.tenant": "tenant-a",
      "vigil.purpose": "claims-triage",
    });
  });

  it("falls back to the global tracer when the caller supplies none", () => {
    const summary = summarise(["a"], {
      ctx: { tenantId: "tenant-a", purpose: "claims-triage" },
      settings,
    });

    expect(summary).toEqual({
      namespace: "claims",
      count: 1,
      truncated: false,
    });
    expect(exporter.getFinishedSpans()).toHaveLength(0);
  });

  it("refuses data-bearing work without a policy context", () => {
    expect(() => summarise(["a"], { ctx: null, settings, tracer })).toThrow(
      /PolicyContext/,
    );
    expect(exporter.getFinishedSpans()).toHaveLength(0);
  });
});
