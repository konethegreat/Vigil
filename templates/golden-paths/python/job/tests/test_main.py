# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Job orchestration and exit-code tests
import pytest

from vigil_job_template.config import Settings
from vigil_job_template.main import (
    EXIT_CONFIG,
    EXIT_OK,
    EXIT_POLICY,
    execute,
    main,
    run,
)
from vigil_job_template.policy import PolicyContext, UnavailableWarden
from vigil_job_template.telemetry import configure_telemetry


def settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "tenant_id": "tenant-1",
        "purpose": "DATA_INTEGRATION",
        "warden_url": "http://warden.invalid:8080",
        "policy_token_ref": "kms://vigil/test/job-token#1",
    }
    values.update(overrides)
    return Settings(**values)  # type: ignore[arg-type]


class _RecordingWarden:
    """Returns a synthetic context and records the call. Used only in tests."""

    def __init__(self) -> None:
        self.calls: list[tuple[str, str, str]] = []

    def authorize(self, *, tenant_id: str, purpose: str, token: str) -> PolicyContext:
        self.calls.append((tenant_id, purpose, token))
        return PolicyContext(tenant_id=tenant_id, purpose=purpose, signed_token="synthetic")


def test_execute_reports_success() -> None:
    ctx = PolicyContext(tenant_id="tenant-1", purpose="DATA_INTEGRATION", signed_token="s")
    assert execute(ctx, settings()) == EXIT_OK


def test_run_passes_the_service_principal_to_warden() -> None:
    warden = _RecordingWarden()
    assert run(settings(), warden) == EXIT_OK
    assert warden.calls == [("tenant-1", "DATA_INTEGRATION", "kms://vigil/test/job-token#1")]


def test_run_stops_when_policy_is_unavailable() -> None:
    assert run(settings(), UnavailableWarden()) == EXIT_POLICY


def test_run_shuts_down_a_configured_exporter() -> None:
    warden = _RecordingWarden()
    loaded = settings(otel_exporter_endpoint="http://collector.invalid:4318/v1/traces")
    assert run(loaded, warden) == EXIT_OK


def test_no_configuration_is_a_config_error() -> None:
    assert main() == EXIT_CONFIG


def test_invalid_configuration_is_a_config_error(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("VIGIL_TENANT_ID", "tenant-env")
    monkeypatch.setenv("VIGIL_PURPOSE", "DATA_INTEGRATION")
    monkeypatch.setenv("VIGIL_WARDEN_URL", "not-a-url")
    monkeypatch.setenv("VIGIL_POLICY_TOKEN_REF", "kms://vigil/test/job-token#1")
    assert main() == EXIT_CONFIG


def test_valid_configuration_reaches_the_fail_closed_stub(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setenv("VIGIL_TENANT_ID", "tenant-env")
    monkeypatch.setenv("VIGIL_PURPOSE", "DATA_INTEGRATION")
    monkeypatch.setenv("VIGIL_WARDEN_URL", "http://warden.invalid:8080")
    monkeypatch.setenv("VIGIL_POLICY_TOKEN_REF", "kms://vigil/test/job-token#1")
    assert main() == EXIT_POLICY


def test_telemetry_is_disabled_without_a_collector() -> None:
    assert configure_telemetry(settings()) is None
