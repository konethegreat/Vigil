# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Validated service configuration
"""Runtime configuration.

Values come from the environment and are validated at startup. Secrets are
KMS references, never literals (BACKEND_GUIDE §9.3, INSTRUCTIONS §9.10).
"""

from typing import Literal

from pydantic import AliasChoices, Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Settings for one service process."""

    model_config = SettingsConfigDict(env_prefix="VIGIL_", extra="forbid", frozen=True)

    service_name: str = Field(default="vigil-service-template", min_length=1, max_length=64)
    environment: Literal["dev", "test", "staging", "production"] = "dev"
    host: str = Field(default="0.0.0.0", min_length=1)  # noqa: S104 - containers bind all interfaces
    port: int = Field(default=8000, ge=1, le=65535)
    log_level: Literal["INFO", "WARNING", "ERROR"] = "INFO"
    warden_url: str = Field(min_length=1)
    internal_token_ref: str = Field(min_length=1)
    otel_exporter_endpoint: str | None = Field(
        default=None,
        validation_alias=AliasChoices(
            "otel_exporter_endpoint",
            "OTEL_EXPORTER_OTLP_ENDPOINT",
            "VIGIL_OTEL_EXPORTER_ENDPOINT",
        ),
    )

    @field_validator("warden_url")
    @classmethod
    def _warden_url_is_http(cls, value: str) -> str:
        if not value.startswith(("http://", "https://")):
            raise ValueError("warden_url must start with http:// or https://")
        return value

    @field_validator("internal_token_ref")
    @classmethod
    def _token_is_a_reference(cls, value: str) -> str:
        if not value.startswith("kms://"):
            raise ValueError("internal_token_ref must be a kms:// reference, never a secret")
        return value

    @field_validator("otel_exporter_endpoint", mode="before")
    @classmethod
    def _empty_endpoint_is_none(cls, value: object) -> object:
        if value == "":
            return None
        return value
