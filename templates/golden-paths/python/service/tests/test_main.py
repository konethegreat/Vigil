# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Health and guarded route tests
import asyncio
from collections.abc import Iterator

import pytest
from fastapi.testclient import TestClient

from vigil_service_template.config import Settings
from vigil_service_template.main import create_app, example
from vigil_service_template.policy import PolicyContext, PolicyUnavailable

_POLICY_HEADERS = {
    "X-Vigil-Tenant": "tenant-1",
    "X-Vigil-Purpose": "REVIEW",
    "X-Vigil-Policy-Token": "synthetic-signed-token",
}


class _ReadyButDenyingWarden:
    """Reports readiness without allowing any request."""

    def ready(self) -> bool:
        return True

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        raise PolicyUnavailable("synthetic outage")


@pytest.fixture
def client() -> Iterator[TestClient]:
    settings = Settings(
        warden_url="http://warden.invalid:8080",
        internal_token_ref="kms://vigil/test/service-auth#1",
    )
    with TestClient(create_app(settings)) as test_client:
        yield test_client


def test_liveness_does_not_check_dependencies(client: TestClient) -> None:
    response = client.get("/healthz/live")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_startup_reports_ok_after_the_lifespan_runs(client: TestClient) -> None:
    assert client.get("/healthz/startup").status_code == 200


def test_readiness_fails_closed_while_warden_is_unavailable(client: TestClient) -> None:
    response = client.get("/healthz/ready")
    assert response.status_code == 503
    assert response.json() == {"status": "not_ready"}


def test_readiness_requires_only_a_ready_binding_not_an_allow() -> None:
    settings = Settings(
        warden_url="http://warden.invalid:8080",
        internal_token_ref="kms://vigil/test/service-auth#1",
    )
    with TestClient(create_app(settings, warden=_ReadyButDenyingWarden())) as client:
        assert client.get("/healthz/ready").status_code == 200


def test_a_configured_collector_instruments_the_app() -> None:
    settings = Settings(
        warden_url="http://warden.invalid:8080",
        internal_token_ref="kms://vigil/test/service-auth#1",
        otel_exporter_endpoint="http://collector.invalid:4318/v1/traces",
    )
    with TestClient(create_app(settings)) as client:
        assert client.get("/healthz/live").status_code == 200


def test_guarded_route_requires_all_policy_headers(client: TestClient) -> None:
    assert client.get("/v1/example").status_code == 400


def test_guarded_route_fails_closed_without_a_warden_binding(client: TestClient) -> None:
    response = client.get("/v1/example", headers=_POLICY_HEADERS)
    assert response.status_code == 503
    assert response.json() == {"detail": "policy evaluation unavailable"}


def test_guarded_route_uses_the_context_it_is_given() -> None:
    ctx = PolicyContext(tenant_id="tenant-1", purpose="REVIEW", signed_token="synthetic")
    assert asyncio.run(example(ctx)) == {"tenant_id": "tenant-1", "purpose": "REVIEW"}
