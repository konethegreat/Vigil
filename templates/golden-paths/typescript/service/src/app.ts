// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Fastify application factory and HTTP surface (P1 task 1.4)
/**
 * Health probes and one guarded example route.
 *
 * Every data-bearing route resolves its {@link PolicyContext} through the
 * Warden binding; missing headers are a 400 and Warden failures keep their own
 * status. Error bodies never echo request input (BACKEND_GUIDE §2.6).
 */
import { trace } from "@opentelemetry/api";
import type { Tracer } from "@opentelemetry/api";
import type { TracerProvider } from "@opentelemetry/sdk-trace";
import Fastify from "fastify";
import type { FastifyInstance, FastifyRequest } from "fastify";

import type { Settings } from "./config.js";
import {
  PolicyDenied,
  PolicyUnavailable,
  UnavailableWarden,
} from "./policy.js";
import type { PolicyContext, WardenBinding } from "./policy.js";

const TENANT_HEADER = "x-vigil-tenant";
const PURPOSE_HEADER = "x-vigil-purpose";
const TOKEN_HEADER = "x-vigil-policy-token";

export class MissingPolicyHeaders extends Error {
  constructor() {
    super("verified policy context required");
    this.name = "MissingPolicyHeaders";
  }
}

export interface AppOptions {
  readonly settings: Settings;
  readonly warden?: WardenBinding;
  readonly provider?: TracerProvider;
  readonly logger?: boolean;
}

function headerValue(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function isPresent(value: string | undefined): value is string {
  return value !== undefined && value.trim().length > 0;
}

function authorize(
  warden: WardenBinding,
  request: FastifyRequest,
): PolicyContext {
  const tenantId = headerValue(request.headers[TENANT_HEADER]);
  const purpose = headerValue(request.headers[PURPOSE_HEADER]);
  const token = headerValue(request.headers[TOKEN_HEADER]);

  if (!isPresent(tenantId) || !isPresent(purpose) || !isPresent(token)) {
    throw new MissingPolicyHeaders();
  }

  return warden.authorize({
    tenantId: tenantId.trim(),
    purpose: purpose.trim(),
    token: token.trim(),
  });
}

export function buildApp(options: AppOptions): FastifyInstance {
  const { settings } = options;
  const warden = options.warden ?? new UnavailableWarden();
  const tracer: Tracer =
    options.provider?.getTracer(settings.serviceName) ??
    trace.getTracer(settings.serviceName);

  const app = Fastify({
    logger:
      options.logger === false
        ? false
        : { level: settings.logLevel.toLowerCase() },
  });

  let started = false;
  app.addHook("onReady", () => {
    started = true;
  });
  app.addHook("onClose", () => {
    started = false;
  });

  app.get("/healthz/live", () => ({ status: "ok" }));

  app.get("/healthz/startup", (_request, reply) => {
    const status = started ? "ok" : "starting";
    return reply.code(started ? 200 : 503).send({ status });
  });

  app.get("/healthz/ready", (_request, reply) => {
    const ready = started && warden.ready();
    return reply
      .code(ready ? 200 : 503)
      .send({ status: ready ? "ready" : "not_ready" });
  });

  app.get("/v1/example", (request) => {
    const context = authorize(warden, request);
    return tracer.startActiveSpan("vigil.service.example", (span) => {
      try {
        span.setAttribute("vigil.tenant", context.tenantId);
        span.setAttribute("vigil.purpose", context.purpose);
        return { tenantId: context.tenantId, purpose: context.purpose };
      } finally {
        span.end();
      }
    });
  });

  app.setErrorHandler((error, request, reply) => {
    if (error instanceof PolicyUnavailable) {
      return reply.code(503).send({ error: "policy decision unavailable" });
    }
    if (error instanceof PolicyDenied) {
      return reply.code(403).send({ error: "policy decision denied" });
    }
    if (error instanceof MissingPolicyHeaders) {
      return reply.code(400).send({ error: error.message });
    }
    request.log.error({ err: error }, "unhandled request error");
    return reply.code(500).send({ error: "internal error" });
  });

  return app;
}
