# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Opt-in OTLP telemetry for jobs
"""Telemetry that never requires egress.

Export only when OTEL_EXPORTER_OTLP_ENDPOINT names an in-boundary collector.
With no endpoint the OpenTelemetry API stays a no-op (INSTRUCTIONS §9.8).
"""

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

from .config import Settings


def configure_telemetry(settings: Settings) -> TracerProvider | None:
    """Install a tracer provider only when a collector is configured."""
    endpoint = settings.otel_exporter_endpoint
    if endpoint is None:
        return None
    provider = TracerProvider(
        resource=Resource.create(
            {
                "service.name": settings.job_name,
                "service.version": "0.1.0",
                "deployment.environment": "job",
            }
        )
    )
    provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=endpoint)))
    trace.set_tracer_provider(provider)
    return provider
