# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Job entry point and exit codes
"""One-shot entry point.

Exit codes are part of the contract: 0 success, 2 invalid configuration, 3
policy unavailable. The template returns 3 until the P3 binding replaces
UnavailableWarden; that is correct fail-closed behaviour, not a bug.
"""

import logging

from opentelemetry import trace
from pydantic import ValidationError

from .config import Settings
from .policy import PolicyContext, PolicyUnavailable, UnavailableWarden, WardenBinding
from .telemetry import configure_telemetry

EXIT_OK = 0
EXIT_CONFIG = 2
EXIT_POLICY = 3

_log = logging.getLogger(__name__)


def execute(ctx: PolicyContext, settings: Settings) -> int:
    """Replace this body with the real batch. It runs only with a context."""
    tracer = trace.get_tracer(__name__)
    with tracer.start_as_current_span("vigil.job.run") as span:
        span.set_attribute("vigil.tenant", ctx.tenant_id)
        span.set_attribute("vigil.purpose", ctx.purpose)
        _log.info("job=%s batch_size=%d", settings.job_name, settings.batch_size)
    return EXIT_OK


def run(settings: Settings, warden: WardenBinding) -> int:
    """Evaluate policy, then run. Never runs without a context."""
    provider = configure_telemetry(settings)
    try:
        try:
            ctx = warden.authorize(
                tenant_id=settings.tenant_id,
                purpose=settings.purpose,
                token=settings.policy_token_ref,
            )
        except PolicyUnavailable:
            _log.error("policy evaluation unavailable; refusing to run")
            return EXIT_POLICY
        return execute(ctx, settings)
    finally:
        if provider is not None:
            provider.shutdown()


def main() -> int:
    try:
        # Required values come from the environment; mypy cannot see that.
        settings = Settings()  # type: ignore[call-arg]
    except ValidationError:
        _log.error("configuration is invalid; see .env.example")
        return EXIT_CONFIG
    logging.basicConfig(level=settings.log_level)
    return run(settings, UnavailableWarden())


if __name__ == "__main__":
    raise SystemExit(main())
