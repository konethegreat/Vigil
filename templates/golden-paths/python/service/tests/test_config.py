# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Configuration validation tests
import pytest
from pydantic import ValidationError

from vigil_service_template.config import Settings


def settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "warden_url": "http://warden.invalid:8080",
        "internal_token_ref": "kms://vigil/test/service-auth#1",
    }
    values.update(overrides)
    return Settings(**values)  # type: ignore[arg-type]


def test_valid_configuration_is_accepted() -> None:
    loaded = settings()
    assert loaded.service_name == "vigil-service-template"
    assert loaded.environment == "dev"
    assert loaded.host == "127.0.0.1"
    assert loaded.port == 8000


def test_warden_url_must_be_http() -> None:
    with pytest.raises(ValidationError, match="warden_url"):
        settings(warden_url="warden:8080")


def test_token_must_be_a_kms_reference() -> None:
    with pytest.raises(ValidationError, match="kms://"):
        settings(internal_token_ref="a-real-secret")


def test_missing_warden_url_fails_fast() -> None:
    with pytest.raises(ValidationError):
        Settings(internal_token_ref="kms://vigil/test/service-auth#1")  # type: ignore[call-arg]


def test_empty_otel_endpoint_disables_export() -> None:
    assert settings(otel_exporter_endpoint="").otel_exporter_endpoint is None


def test_environment_aliases_are_read(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("VIGIL_WARDEN_URL", "https://warden.internal")
    monkeypatch.setenv("VIGIL_INTERNAL_TOKEN_REF", "kms://vigil/test/service-auth#2")
    monkeypatch.setenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector.internal:4318/v1/traces")
    loaded = Settings()  # type: ignore[call-arg]
    assert loaded.warden_url == "https://warden.internal"
    assert loaded.otel_exporter_endpoint == "http://collector.internal:4318/v1/traces"
