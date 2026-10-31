# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Policy dependency mapping tests
import pytest
from fastapi import HTTPException

from vigil_service_template.deps import get_policy_context
from vigil_service_template.policy import PolicyContext, PolicyDenied, PolicyUnavailable


class _DenyingWarden:
    def ready(self) -> bool:
        return False

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        raise PolicyDenied("synthetic denial")


class _BrokenWarden:
    def ready(self) -> bool:
        return False

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        raise PolicyUnavailable("synthetic outage")


def test_missing_context_headers_are_a_bad_request() -> None:
    with pytest.raises(HTTPException) as raised:
        get_policy_context(
            _BrokenWarden(), x_vigil_tenant="", x_vigil_purpose="", x_vigil_policy_token=""
        )
    assert raised.value.status_code == 400


def test_policy_unavailable_maps_to_service_unavailable() -> None:
    with pytest.raises(HTTPException) as raised:
        get_policy_context(
            _BrokenWarden(), x_vigil_tenant="t", x_vigil_purpose="p", x_vigil_policy_token="s"
        )
    assert raised.value.status_code == 503


def test_policy_denied_maps_to_forbidden() -> None:
    with pytest.raises(HTTPException) as raised:
        get_policy_context(
            _DenyingWarden(), x_vigil_tenant="t", x_vigil_purpose="p", x_vigil_policy_token="s"
        )
    assert raised.value.status_code == 403
