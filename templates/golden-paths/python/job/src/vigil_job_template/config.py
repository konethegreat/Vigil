# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Validated job configuration
"""One-shot job configuration.

Secrets are KMS references only. A job runs as a service principal with a
narrow purpose and is audited like a user (BACKEND_GUIDE §9.1).
"""

from typing import Literal

from pydantic import AliasChoices, Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Settings for one job invocation."""

    model_config = SettingsConfigDict(env_prefix="VIGIL_", extra="forbid", frozen=True)

    job_name: str = Field(default="vigil-job-template", min_length=1, max_length=64)
    tenant_id: str = Field(min_length=1)
    purpose: str = Field(min_length=1)
    warden_url: str = Field(min_length=1)
    policy_token_ref: str = Field(min_length=1)
    batch_size: int = Field(default=100, ge=1, le=10_000)
    log_level: Literal["INFO", "WARNING", "ERROR"] = "INFO"
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

    @field_validator("policy_token_ref")
    @classmethod
    def _token_is_a_reference(cls, value: str) -> str:
        if not value.startswith("kms://"):
            raise ValueError("policy_token_ref must be a kms:// reference, never a secret")
        return value

    @field_validator("otel_exporter_endpoint", mode="before")
    @classmethod
    def _empty_endpoint_is_none(cls, value: object) -> object:
        if value == "":
            return None
        return value
