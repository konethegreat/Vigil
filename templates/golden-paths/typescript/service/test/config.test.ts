// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Validated configuration tests (TDD)
import { describe, expect, it } from "vitest";

import { ConfigurationError, loadSettings } from "../src/index.js";

const REQUIRED = {
  VIGIL_WARDEN_URL: "http://warden.invalid:8080",
  VIGIL_INTERNAL_TOKEN_REF: "kms://vigil/test/service#1",
};

describe("loadSettings", () => {
  it("applies the documented defaults", () => {
    expect(loadSettings(REQUIRED)).toEqual({
      serviceName: "vigil-service-template",
      environment: "dev",
      host: "127.0.0.1",
      port: 8080,
      logLevel: "INFO",
      wardenUrl: "http://warden.invalid:8080",
      internalTokenRef: "kms://vigil/test/service#1",
      otelEndpoint: undefined,
    });
  });

  it("reads every documented environment variable", () => {
    const settings = loadSettings({
      ...REQUIRED,
      VIGIL_SERVICE_NAME: "claims-api",
      VIGIL_ENVIRONMENT: "staging",
      VIGIL_HOST: "0.0.0.0",
      VIGIL_PORT: "9000",
      VIGIL_LOG_LEVEL: "WARNING",
      OTEL_EXPORTER_OTLP_ENDPOINT: "http://collector.invalid:4318/v1/traces",
    });

    expect(settings).toMatchObject({
      serviceName: "claims-api",
      environment: "staging",
      host: "0.0.0.0",
      port: 9000,
      logLevel: "WARNING",
      otelEndpoint: "http://collector.invalid:4318/v1/traces",
    });
  });

  it("rejects a service name longer than 64 characters", () => {
    expect(() =>
      loadSettings({ ...REQUIRED, VIGIL_SERVICE_NAME: "a".repeat(65) }),
    ).toThrow(/serviceName/);
  });

  it("requires a Warden URL", () => {
    expect(() =>
      loadSettings({
        VIGIL_INTERNAL_TOKEN_REF: REQUIRED.VIGIL_INTERNAL_TOKEN_REF,
      }),
    ).toThrow(/wardenUrl/);
  });

  it("requires an internal token reference", () => {
    expect(() =>
      loadSettings({ VIGIL_WARDEN_URL: REQUIRED.VIGIL_WARDEN_URL }),
    ).toThrow(/internalTokenRef/);
  });

  it("rejects a Warden URL that is not http", () => {
    expect(() =>
      loadSettings({ ...REQUIRED, VIGIL_WARDEN_URL: "warden.invalid:8080" }),
    ).toThrow(/wardenUrl/);
  });

  it("refuses a token that is not a kms reference", () => {
    expect(() =>
      loadSettings({ ...REQUIRED, VIGIL_INTERNAL_TOKEN_REF: "a-real-secret" }),
    ).toThrow(/internalTokenRef/);
  });

  it.each(["0", "70000", "eighty", "8080.5"])("rejects the port %s", (port) => {
    expect(() => loadSettings({ ...REQUIRED, VIGIL_PORT: port })).toThrow(
      /port/,
    );
  });

  it("rejects an unknown environment", () => {
    expect(() =>
      loadSettings({ ...REQUIRED, VIGIL_ENVIRONMENT: "prod" }),
    ).toThrow(/environment/);
  });

  it("rejects an unknown log level", () => {
    expect(() =>
      loadSettings({ ...REQUIRED, VIGIL_LOG_LEVEL: "DEBUG" }),
    ).toThrow(/logLevel/);
  });

  it("treats a blank OTLP endpoint as off", () => {
    expect(
      loadSettings({ ...REQUIRED, OTEL_EXPORTER_OTLP_ENDPOINT: "  " })
        .otelEndpoint,
    ).toBeUndefined();
  });

  it("rejects an OTLP endpoint that is not http", () => {
    expect(() =>
      loadSettings({
        ...REQUIRED,
        OTEL_EXPORTER_OTLP_ENDPOINT: "collector.invalid:4318",
      }),
    ).toThrow(/otelEndpoint/);
  });

  it("prefers VIGIL_OTEL_ENDPOINT over the standard variable", () => {
    const settings = loadSettings({
      ...REQUIRED,
      VIGIL_OTEL_ENDPOINT: "http://vigil-collector:4318/v1/traces",
      OTEL_EXPORTER_OTLP_ENDPOINT: "http://otel-collector:4318/v1/traces",
    });

    expect(settings.otelEndpoint).toBe("http://vigil-collector:4318/v1/traces");
  });

  it("reports every problem at once", () => {
    try {
      loadSettings({
        VIGIL_WARDEN_URL: "nope",
        VIGIL_INTERNAL_TOKEN_REF: "nope",
      });
      expect.unreachable("loadSettings should have thrown");
    } catch (error) {
      expect(error).toBeInstanceOf(ConfigurationError);
      expect((error as ConfigurationError).issues).toHaveLength(2);
    }
  });
});
