# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Fail-closed policy tests
import pytest

from vigil_job_template.policy import PolicyUnavailable, UnavailableWarden


def test_stub_binding_never_allows() -> None:
    with pytest.raises(PolicyUnavailable):
        UnavailableWarden().authorize(
            tenant_id="tenant-1", purpose="DATA_INTEGRATION", token="synthetic"
        )
