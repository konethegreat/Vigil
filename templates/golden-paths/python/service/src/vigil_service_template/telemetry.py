# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Opt-in OTLP telemetry
"""Telemetry that never requires egress.

Spans are exported only when OTEL_EXPORTER_OTLP_ENDPOINT names an in-boundary
collector. With no endpoint the OpenTelemetry API stays a no-op, so the service
runs offline (INSTRUCTIONS §9.8, BACKEND_GUIDE §9.2).
"""

from fastapi import FastAPI
from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

from .config import Settings

_HEALTH_PATHS = "healthz/live,healthz/ready,healthz/startup"


def configure_telemetry(settings: Settings) -> TracerProvider | None:
    """Install a tracer provider only when a collector is configured."""
    endpoint = settings.otel_exporter_endpoint
    if endpoint is None:
        return None
    provider = TracerProvider(
        resource=Resource.create(
            {
                "service.name": settings.service_name,
                "service.version": "0.1.0",
                "deployment.environment": settings.environment,
            }
        )
    )
    provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=endpoint)))
    trace.set_tracer_provider(provider)
    return provider


def instrument_app(app: FastAPI) -> None:
    """Trace requests without recording health probes."""
    FastAPIInstrumentor.instrument_app(app, excluded_urls=_HEALTH_PATHS)
