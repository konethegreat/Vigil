# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Telemetry opt-in tests
from vigil_service_template.config import Settings
from vigil_service_template.telemetry import configure_telemetry


def settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "warden_url": "http://warden.invalid:8080",
        "internal_token_ref": "kms://vigil/test/service-auth#1",
    }
    values.update(overrides)
    return Settings(**values)  # type: ignore[arg-type]


def test_no_collector_means_no_export() -> None:
    assert configure_telemetry(settings()) is None


def test_configured_collector_installs_a_provider() -> None:
    provider = configure_telemetry(
        settings(otel_exporter_endpoint="http://collector.internal:4318/v1/traces")
    )
    assert provider is not None
    provider.shutdown()
