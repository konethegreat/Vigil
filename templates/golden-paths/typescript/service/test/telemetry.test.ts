// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Opt-in OTLP telemetry tests (TDD)
import type { TracerProvider } from "@opentelemetry/sdk-trace";
import { afterEach, describe, expect, it } from "vitest";

import { loadSettings, otlpExporter, startTelemetry } from "../src/index.js";

const REQUIRED = {
  VIGIL_WARDEN_URL: "http://warden.invalid:8080",
  VIGIL_INTERNAL_TOKEN_REF: "kms://vigil/test/service#1",
};

describe("otlpExporter", () => {
  it("builds no exporter without an endpoint", () => {
    expect(otlpExporter(loadSettings(REQUIRED))).toBeUndefined();
  });

  it("builds an exporter for the configured collector", () => {
    const settings = loadSettings({
      ...REQUIRED,
      OTEL_EXPORTER_OTLP_ENDPOINT: "http://collector.invalid:4318/v1/traces",
    });

    expect(otlpExporter(settings)).toBeDefined();
  });
});

describe("startTelemetry", () => {
  const started: TracerProvider[] = [];

  afterEach(async () => {
    await Promise.all(started.splice(0).map((provider) => provider.shutdown()));
  });

  it("stays off without an endpoint, so the process needs no egress", () => {
    expect(startTelemetry(loadSettings(REQUIRED))).toBeUndefined();
  });

  it("installs a provider when a collector is configured", () => {
    const provider = startTelemetry(
      loadSettings({
        ...REQUIRED,
        OTEL_EXPORTER_OTLP_ENDPOINT: "http://collector.invalid:4318/v1/traces",
      }),
    );

    expect(provider).toBeDefined();
    if (provider !== undefined) {
      started.push(provider);
    }
  });
});
