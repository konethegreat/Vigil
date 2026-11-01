// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — HTTP surface tests (TDD)
import {
  InMemorySpanExporter,
  SimpleSpanProcessor,
  TracerProvider,
} from "@opentelemetry/sdk-trace";
import type { FastifyInstance } from "fastify";
import { afterEach, describe, expect, it } from "vitest";

import {
  PolicyDenied,
  PolicyUnavailable,
  buildApp,
  loadSettings,
} from "../src/index.js";
import type { PolicyContext, WardenBinding } from "../src/index.js";

const settings = loadSettings({
  VIGIL_WARDEN_URL: "http://warden.invalid:8080",
  VIGIL_INTERNAL_TOKEN_REF: "kms://vigil/test/service#1",
});

const allowingWarden: WardenBinding = {
  ready: () => true,
  authorize: ({ tenantId, purpose, token }) => ({
    tenantId,
    purpose,
    signedToken: token,
  }),
};

const denyingWarden: WardenBinding = {
  ready: () => true,
  authorize: () => {
    throw new PolicyDenied("policy refused the request");
  },
};

const brokenWarden: WardenBinding = {
  ready: () => false,
  authorize: () => {
    throw new PolicyUnavailable("warden is unreachable");
  },
};

const unexpectedWarden: WardenBinding = {
  ready: () => true,
  authorize: () => {
    throw new Error("boom");
  },
};

const headers = {
  "x-vigil-tenant": "tenant-a",
  "x-vigil-purpose": "claims-triage",
  "x-vigil-policy-token": "signed-token",
};

const apps: FastifyInstance[] = [];

function appWith(
  warden?: WardenBinding,
  provider?: TracerProvider,
): FastifyInstance {
  const app = buildApp({
    settings,
    logger: false,
    ...(warden === undefined ? {} : { warden }),
    ...(provider === undefined ? {} : { provider }),
  });
  apps.push(app);
  return app;
}

afterEach(async () => {
  await Promise.all(apps.splice(0).map((app) => app.close()));
});

describe("health probes", () => {
  it("reports live", async () => {
    const response = await appWith(allowingWarden).inject({
      method: "GET",
      url: "/healthz/live",
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({ status: "ok" });
  });

  it("reports startup complete once the app is ready", async () => {
    const response = await appWith(allowingWarden).inject({
      method: "GET",
      url: "/healthz/startup",
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({ status: "ok" });
  });

  it("stays not ready while the Warden binding is unavailable", async () => {
    const response = await appWith().inject({
      method: "GET",
      url: "/healthz/ready",
    });

    expect(response.statusCode).toBe(503);
    expect(response.json()).toEqual({ status: "not_ready" });
  });

  it("reports ready when policy evaluation is available", async () => {
    const response = await appWith(allowingWarden).inject({
      method: "GET",
      url: "/healthz/ready",
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({ status: "ready" });
  });
});

describe("GET /v1/example", () => {
  it("requires all three verified headers", async () => {
    const response = await appWith(allowingWarden).inject({
      method: "GET",
      url: "/v1/example",
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toEqual({
      error: "verified policy context required",
    });
  });

  it("rejects blank header values", async () => {
    const response = await appWith(allowingWarden).inject({
      method: "GET",
      url: "/v1/example",
      headers: { ...headers, "x-vigil-tenant": "  " },
    });

    expect(response.statusCode).toBe(400);
  });

  it("returns the context Warden authorises", async () => {
    const response = await appWith(allowingWarden).inject({
      method: "GET",
      url: "/v1/example",
      headers,
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      tenantId: "tenant-a",
      purpose: "claims-triage",
    });
  });

  it("fails closed with 503 while the Warden binding is unavailable", async () => {
    const response = await appWith(brokenWarden).inject({
      method: "GET",
      url: "/v1/example",
      headers,
    });

    expect(response.statusCode).toBe(503);
    expect(response.json()).toEqual({ error: "policy decision unavailable" });
  });

  it("returns 403 when policy denies the request", async () => {
    const response = await appWith(denyingWarden).inject({
      method: "GET",
      url: "/v1/example",
      headers,
    });

    expect(response.statusCode).toBe(403);
    expect(response.json()).toEqual({ error: "policy decision denied" });
  });

  it("never echoes the header values in an error", async () => {
    const response = await appWith(brokenWarden).inject({
      method: "GET",
      url: "/v1/example",
      headers: { ...headers, "x-vigil-tenant": "tenant-secret-value" },
    });

    expect(response.body).not.toContain("tenant-secret-value");
  });

  it("never relates internal failures to the caller", async () => {
    const response = await appWith(unexpectedWarden).inject({
      method: "GET",
      url: "/v1/example",
      headers,
    });

    expect(response.statusCode).toBe(500);
    expect(response.json()).toEqual({ error: "internal error" });
    expect(response.body).not.toContain("boom");
  });

  it("records tenant and purpose on a span and nothing else", async () => {
    const exporter = new InMemorySpanExporter();
    const provider = new TracerProvider({
      spanProcessors: [new SimpleSpanProcessor({ exporter })],
    });

    const response = await appWith(allowingWarden, provider).inject({
      method: "GET",
      url: "/v1/example",
      headers,
    });

    expect(response.statusCode).toBe(200);
    const [span] = exporter.getFinishedSpans();
    expect(span?.name).toBe("vigil.service.example");
    expect(span?.attributes).toEqual({
      "vigil.tenant": "tenant-a",
      "vigil.purpose": "claims-triage",
    });
  });
});

describe("buildApp", () => {
  it("starts with the fail-closed Warden stub when none is injected", async () => {
    const response = await appWith().inject({
      method: "GET",
      url: "/v1/example",
      headers: {
        "x-vigil-tenant": "tenant-a",
        "x-vigil-purpose": "claims-triage",
        "x-vigil-policy-token": "signed-token",
      } satisfies Record<string, string>,
    });

    expect(response.statusCode).toBe(503);
  });

  it("accepts the context type the binding returns", () => {
    const context: PolicyContext = {
      tenantId: "tenant-a",
      purpose: "claims-triage",
      signedToken: "signed-token",
    };

    expect(context.tenantId).toBe("tenant-a");
  });
});
