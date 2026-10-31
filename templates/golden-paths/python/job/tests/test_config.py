# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Job configuration tests
import pytest
from pydantic import ValidationError

from vigil_job_template.config import Settings


def settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "tenant_id": "tenant-1",
        "purpose": "DATA_INTEGRATION",
        "warden_url": "http://warden.invalid:8080",
        "policy_token_ref": "kms://vigil/test/job-token#1",
    }
    values.update(overrides)
    return Settings(**values)  # type: ignore[arg-type]


def test_valid_configuration_is_accepted() -> None:
    loaded = settings()
    assert loaded.job_name == "vigil-job-template"
    assert loaded.batch_size == 100


def test_warden_url_must_be_http() -> None:
    with pytest.raises(ValidationError, match="warden_url"):
        settings(warden_url="warden:8080")


def test_token_must_be_a_kms_reference() -> None:
    with pytest.raises(ValidationError, match="kms://"):
        settings(policy_token_ref="a-real-secret")


def test_batch_size_must_be_positive() -> None:
    with pytest.raises(ValidationError):
        settings(batch_size=0)


def test_missing_identity_fails_fast() -> None:
    with pytest.raises(ValidationError):
        Settings(  # type: ignore[call-arg]
            purpose="DATA_INTEGRATION",
            warden_url="http://warden.invalid:8080",
            policy_token_ref="kms://vigil/test/job-token#1",
        )


def test_empty_otel_endpoint_disables_export() -> None:
    assert settings(otel_exporter_endpoint="").otel_exporter_endpoint is None


def test_environment_aliases_are_read(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("VIGIL_TENANT_ID", "tenant-env")
    monkeypatch.setenv("VIGIL_PURPOSE", "DATA_INTEGRATION")
    monkeypatch.setenv("VIGIL_WARDEN_URL", "https://warden.internal")
    monkeypatch.setenv("VIGIL_POLICY_TOKEN_REF", "kms://vigil/test/job-token#2")
    monkeypatch.setenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector.internal:4318/v1/traces")
    loaded = Settings()  # type: ignore[call-arg]
    assert loaded.tenant_id == "tenant-env"
    assert loaded.otel_exporter_endpoint == "http://collector.internal:4318/v1/traces"
