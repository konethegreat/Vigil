# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Test environment isolation
"""Keep the developer's shell environment out of template tests."""

import pytest

_CLEARED = (
    "VIGIL_WARDEN_URL",
    "VIGIL_INTERNAL_TOKEN_REF",
    "VIGIL_OTEL_EXPORTER_ENDPOINT",
    "OTEL_EXPORTER_OTLP_ENDPOINT",
)


@pytest.fixture(autouse=True)
def clean_environment(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in _CLEARED:
        monkeypatch.delenv(name, raising=False)
