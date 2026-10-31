# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Policy context contract for libraries
"""Policy context as a structural contract.

The caller obtains a context from Warden and passes it explicitly. A library
never builds one, never reads ambient state and never allows a data-bearing
call without one (INSTRUCTIONS §9.1).
"""

from typing import Protocol


class PolicyContext(Protocol):
    """What the library needs from the Warden-issued context."""

    tenant_id: str
    purpose: str


class MissingPolicyContext(ValueError):
    """A data-bearing call was made without a usable policy context."""


def require_policy_context(ctx: PolicyContext | None) -> PolicyContext:
    """Return the caller's context or fail closed."""
    if ctx is None:
        raise MissingPolicyContext("a PolicyContext is required for data-bearing work")
    if not ctx.tenant_id.strip() or not ctx.purpose.strip():
        raise MissingPolicyContext("the PolicyContext must carry a tenant and purpose")
    return ctx
