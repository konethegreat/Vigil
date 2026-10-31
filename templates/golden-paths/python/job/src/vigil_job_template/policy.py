# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Fail-closed Warden binding stub
"""Warden binding stub for jobs.

The real binding arrives with P3. Until then every invocation fails closed,
which is the correct behaviour for a data-bearing job (INSTRUCTIONS §9.1, B4).
"""

from dataclasses import dataclass
from typing import Protocol


class PolicyUnavailable(RuntimeError):
    """Policy could not be evaluated. The job must not proceed."""


class PolicyDenied(RuntimeError):
    """Policy evaluated and refused the run."""


@dataclass(frozen=True, slots=True)
class PolicyContext:
    """The signed context that authorises one job run."""

    tenant_id: str
    purpose: str
    signed_token: str


class WardenBinding(Protocol):
    """The seam the P3 binding implements."""

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        """Evaluate policy and return a context, or raise."""
        ...


class UnavailableWarden:
    """Placeholder binding. It never allows a run."""

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        raise PolicyUnavailable("the Warden binding is not wired yet")
