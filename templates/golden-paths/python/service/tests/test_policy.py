# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Fail-closed policy tests
import pytest

from vigil_service_template.policy import PolicyUnavailable, UnavailableWarden


def test_stub_binding_is_never_ready() -> None:
    assert UnavailableWarden().ready() is False


def test_stub_binding_never_allows() -> None:
    with pytest.raises(PolicyUnavailable):
        UnavailableWarden().authorize(tenant_id="tenant-1", purpose="REVIEW", token="signed")
