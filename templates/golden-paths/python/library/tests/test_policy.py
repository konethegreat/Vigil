# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Policy context contract tests
import pytest

from vigil_library_template.policy import (
    MissingPolicyContext,
    PolicyContext,
    require_policy_context,
)


class _Context:
    def __init__(self, tenant_id: str, purpose: str) -> None:
        self.tenant_id = tenant_id
        self.purpose = purpose


def test_context_is_returned_unchanged() -> None:
    ctx = _Context("tenant-1", "REVIEW")
    assert require_policy_context(ctx) is ctx


def test_missing_context_fails_closed() -> None:
    with pytest.raises(MissingPolicyContext, match="required"):
        require_policy_context(None)


def test_context_without_a_tenant_fails_closed() -> None:
    with pytest.raises(MissingPolicyContext, match="tenant and purpose"):
        require_policy_context(_Context("", "REVIEW"))


def test_context_without_a_purpose_fails_closed() -> None:
    with pytest.raises(MissingPolicyContext, match="tenant and purpose"):
        require_policy_context(_Context("tenant-1", " "))


def test_context_satisfies_the_protocol() -> None:
    ctx: PolicyContext = _Context("tenant-1", "REVIEW")
    assert ctx.tenant_id == "tenant-1"
