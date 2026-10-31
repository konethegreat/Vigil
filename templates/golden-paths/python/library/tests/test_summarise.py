# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Summarise operation tests
import pytest

from vigil_library_template.config import LibrarySettings
from vigil_library_template.policy import MissingPolicyContext
from vigil_library_template.summarise import summarise


class _Context:
    tenant_id = "tenant-1"
    purpose = "REVIEW"


def test_counts_only_non_empty_records() -> None:
    settings = LibrarySettings(namespace="synthetic", batch_size=5)
    result = summarise([" alpha ", "", "beta", "   "], ctx=_Context(), settings=settings)
    assert result == {"namespace": "synthetic", "count": 2, "truncated": False}


def test_truncation_is_reported() -> None:
    settings = LibrarySettings(namespace="synthetic", batch_size=1)
    result = summarise(["alpha", "beta"], ctx=_Context(), settings=settings)
    assert result["truncated"] is True


def test_empty_input_is_not_truncated() -> None:
    settings = LibrarySettings(namespace="synthetic")
    result = summarise([], ctx=_Context(), settings=settings)
    assert result["count"] == 0
    assert result["truncated"] is False


def test_missing_context_fails_closed_before_reading_records() -> None:
    settings = LibrarySettings(namespace="synthetic")
    with pytest.raises(MissingPolicyContext):
        summarise(["alpha"], ctx=None, settings=settings)
