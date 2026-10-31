# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Python library golden path package
"""Golden-path Python library."""

from .config import LibrarySettings
from .policy import MissingPolicyContext, PolicyContext, require_policy_context
from .summarise import summarise

__all__ = [
    "LibrarySettings",
    "MissingPolicyContext",
    "PolicyContext",
    "require_policy_context",
    "summarise",
]
__version__ = "0.1.0"
