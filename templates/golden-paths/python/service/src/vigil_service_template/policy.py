# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Fail-closed Warden binding stub
"""Warden binding stub.

The real binding arrives with P3. Until then this stub refuses every request,
which is the correct fail-closed behaviour (INSTRUCTIONS §9.1, B4). Replace it
with the P3 binding; never add an allow-all path.
"""

from dataclasses import dataclass
from typing import Protocol


class PolicyUnavailable(RuntimeError):
    """Policy could not be evaluated. The caller must fail closed."""


class PolicyDenied(RuntimeError):
    """Policy evaluated and refused the request."""


@dataclass(frozen=True, slots=True)
class PolicyContext:
    """The signed context that authorises one request."""

    tenant_id: str
    purpose: str
    signed_token: str


class WardenBinding(Protocol):
    """The seam the P3 binding implements."""

    def ready(self) -> bool:
        """Return True only when policy evaluation is available."""
        ...

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        """Evaluate policy and return a context, or raise."""
        ...


class UnavailableWarden:
    """Placeholder binding. It is never ready and never allows."""

    def ready(self) -> bool:
        return False

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        raise PolicyUnavailable("the Warden binding is not wired yet")
