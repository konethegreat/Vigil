# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Example library operation
"""An example operation that consumes, but never builds, policy context."""

from collections.abc import Sequence

from opentelemetry import trace

from .config import LibrarySettings
from .policy import PolicyContext, require_policy_context

_tracer = trace.get_tracer(__name__)


def summarise(
    records: Sequence[str],
    *,
    ctx: PolicyContext | None,
    settings: LibrarySettings,
) -> dict[str, object]:
    """Count non-empty records without recording their content.

    Only the tenant and purpose are attached to the span; record values stay
    out of telemetry (BACKEND_GUIDE §9.2, B5).
    """
    context = require_policy_context(ctx)
    with _tracer.start_as_current_span("vigil.library.summarise") as span:
        span.set_attribute("vigil.tenant", context.tenant_id)
        span.set_attribute("vigil.purpose", context.purpose)
        cleaned = [record.strip() for record in records if record.strip()]
        return {
            "namespace": settings.namespace,
            "count": len(cleaned),
            "truncated": len(cleaned) > settings.batch_size,
        }
