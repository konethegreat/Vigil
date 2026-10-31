# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Injected library configuration
"""Library settings are supplied by the caller.

A library never reads the environment: the service or job that imports it owns
configuration and validation (BACKEND_GUIDE §2.4, §9.3).
"""

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class LibrarySettings:
    """Configuration for one caller's use of the library."""

    namespace: str
    batch_size: int = 100

    def __post_init__(self) -> None:
        if not self.namespace.strip():
            raise ValueError("namespace must not be empty")
        if self.batch_size < 1:
            raise ValueError("batch_size must be positive")
