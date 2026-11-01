// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Opt-in OTLP telemetry (P1 task 1.4)
/**
 * Telemetry that never requires egress.
 *
 * Spans are exported only when an in-boundary collector endpoint is
 * configured. With no endpoint the OpenTelemetry API stays a no-op, so the
 * service runs offline (INSTRUCTIONS §9.8, BACKEND_GUIDE §9.2).
 */
import { trace } from "@opentelemetry/api";
import { OTLPTraceExporter } from "@opentelemetry/exporter-trace-otlp-http";
import { resourceFromAttributes } from "@opentelemetry/resources";
import { BatchSpanProcessor, TracerProvider } from "@opentelemetry/sdk-trace";
import type { SpanExporter } from "@opentelemetry/sdk-trace";

import type { Settings } from "./config.js";

export function otlpExporter(settings: Settings): SpanExporter | undefined {
  if (settings.otelEndpoint === undefined) {
    return undefined;
  }
  return new OTLPTraceExporter({ url: settings.otelEndpoint });
}

export function startTelemetry(settings: Settings): TracerProvider | undefined {
  const spanExporter = otlpExporter(settings);
  if (spanExporter === undefined) {
    return undefined;
  }

  const provider = new TracerProvider({
    resource: resourceFromAttributes({
      "service.name": settings.serviceName,
      "service.version": "0.1.0",
      "deployment.environment": settings.environment,
    }),
    spanProcessors: [new BatchSpanProcessor({ exporter: spanExporter })],
  });
  trace.setGlobalTracerProvider(provider);
  return provider;
}
