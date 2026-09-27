# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Mind boundary tests
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
import json

import httpx
import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from vigil_mind.contracts import Analysis, Challenge, PlanRequest, validate_references
from vigil_mind.main import app
from vigil_mind.planner import Planner, baseline


@pytest.fixture(autouse=True)
def local_config(monkeypatch):
    monkeypatch.setenv("VIGIL_OLLAMA_URL", "http://localhost:11434")
    monkeypatch.setenv("VIGIL_REASONER_MODEL", "")
    monkeypatch.setenv("VIGIL_CHALLENGER_MODEL", "")
    monkeypatch.setenv("VIGIL_INTERNAL_TOKEN", "local-synthetic-mission-only")


@pytest.fixture
def snapshot():
    return PlanRequest(
        mission_id="VGL-TEST",
        domain="LIFELINES",
        objective="Restore the fictional service safely.",
        evidence_version=3,
        evidence=[
            {
                "id": "SRC-TEST",
                "title": "Synthetic inspection",
                "statement": "The outage time conflicts with another record.",
                "status": "DISPUTED",
                "sha256": "a" * 64,
            }
        ],
    )


def client(handler):
    return httpx.AsyncClient(transport=httpx.MockTransport(handler))


@pytest.mark.asyncio
async def test_baseline_does_not_call_a_model(snapshot):
    def forbidden(_):
        raise AssertionError("A baseline must not invoke a model")

    async with client(forbidden) as http:
        result = await Planner(http).plan(snapshot)
    assert result["mode"] == "BASELINE"
    assert result["model"] is None
    assert result["review"]["state"] == "NOT_RUN"
    assert result["evidence_version"] == 3
    assert result["hypotheses"][0]["assessment"] == "DISPUTED"
    assert "deterministic" in result["warnings"][0]


def test_protection_preserves_safe_contact_and_uncertainty(snapshot):
    protected = snapshot.model_copy(update={"domain": "PROTECT"})
    protected.evidence[0].status = "UNVERIFIED"
    analysis = baseline(protected)
    assert "safe contact" in analysis.next_steps[0]
    assert "practitioner" in analysis.summary
    assert analysis.hypotheses[0].assessment == "INSUFFICIENT_EVIDENCE"
    assert any("Confirm" in gap for gap in analysis.gaps)


@pytest.mark.asyncio
async def test_reasoner_and_challenger_use_separate_validated_calls(snapshot, monkeypatch):
    monkeypatch.setenv("VIGIL_REASONER_MODEL", "model-a")
    monkeypatch.setenv("VIGIL_CHALLENGER_MODEL", "model-b")
    calls = []

    def respond(request):
        payload = json.loads(request.content)
        calls.append(payload)
        output = (
            baseline(snapshot).model_dump()
            if payload["model"] == "model-a"
            else {"concerns": ["Confirm the conflicting timeline."]}
        )
        return httpx.Response(200, json={"message": {"content": json.dumps(output)}})

    async with client(respond) as http:
        result = await Planner(http).plan(snapshot)
    assert result["mode"] == "LOCAL_MODEL"
    assert result["review"]["state"] == "COMPLETE"
    assert [call["model"] for call in calls] == ["model-a", "model-b"]
    assert "<untrusted_content" in calls[0]["messages"][1]["content"]
    assert "tools" not in calls[0]
    assert result["support_check"] == "HUMAN_REVIEW_REQUIRED"


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "failure",
    ["unknown_source", "invalid_json", "wrong_schema", "oversized", "timeout", "http_error"],
)
async def test_invalid_or_unavailable_reasoner_falls_back(snapshot, monkeypatch, failure):
    monkeypatch.setenv("VIGIL_REASONER_MODEL", "model-a")

    def respond(request):
        if failure == "timeout":
            raise httpx.ReadTimeout("unavailable", request=request)
        if failure == "http_error":
            return httpx.Response(503)
        output = baseline(snapshot).model_dump()
        if failure == "unknown_source":
            output["hypotheses"][0]["evidence_ids"] = ["SRC-NOT-IN-MISSION"]
        if failure == "wrong_schema":
            output["execute_tool"] = "forbidden"
        content = (
            "invalid"
            if failure == "invalid_json"
            else "a" * 24001
            if failure == "oversized"
            else json.dumps(output)
        )
        return httpx.Response(200, json={"message": {"content": content}})

    async with client(respond) as http:
        result = await Planner(http).plan(snapshot)
    assert result["mode"] == "BASELINE"
    assert result["model"] is None
    assert result["hypotheses"][0]["evidence_ids"] == ["SRC-TEST"]
    assert result["warnings"]


@pytest.mark.asyncio
async def test_failed_challenger_does_not_claim_review(snapshot, monkeypatch):
    monkeypatch.setenv("VIGIL_REASONER_MODEL", "model-a")
    monkeypatch.setenv("VIGIL_CHALLENGER_MODEL", "model-b")

    def respond(request):
        if json.loads(request.content)["model"] == "model-b":
            return httpx.Response(503)
        return httpx.Response(
            200, json={"message": {"content": baseline(snapshot).model_dump_json()}}
        )

    async with client(respond) as http:
        result = await Planner(http).plan(snapshot)
    assert result["mode"] == "LOCAL_MODEL"
    assert result["review"]["state"] == "UNAVAILABLE"
    assert "review could not complete" in result["warnings"][0]


@pytest.mark.asyncio
@pytest.mark.parametrize("available", [True, False])
async def test_status_reports_actual_configuration(monkeypatch, available):
    monkeypatch.setenv("VIGIL_REASONER_MODEL", "model-a")
    monkeypatch.setenv("VIGIL_CHALLENGER_MODEL", "model-b")
    async with client(
        lambda _: (
            httpx.Response(200, json={"models": [{"name": "model-a"}, {"name": "model-b"}]})
            if available
            else httpx.Response(503)
        )
    ) as http:
        result = await Planner(http).status()
    assert result["state"] == ("READY" if available else "UNAVAILABLE")
    assert result["challenger_ready"] == available


@pytest.mark.parametrize(
    "url",
    [
        "https://external.example",
        "http://localhost:11434/secret",
        "http://user:password@localhost:11434",
        "http://localhost:11434?x=1",
    ],
)
def test_nonlocal_and_ambiguous_destinations_rejected(monkeypatch, url):
    monkeypatch.setenv("VIGIL_OLLAMA_URL", url)
    with pytest.raises(ValueError, match="local Ollama"):
        Planner(httpx.AsyncClient())


def test_contract_rejects_unknown_fields_and_large_concerns(snapshot):
    with pytest.raises(ValidationError):
        PlanRequest.model_validate({**snapshot.model_dump(), "tenant": "arbitrary"})
    with pytest.raises(ValidationError):
        Challenge(concerns=["x" * 1601])


def test_oversized_analysis_steps_rejected(snapshot):
    analysis = baseline(snapshot).model_dump()
    analysis["next_steps"] = ["x" * 1601]
    with pytest.raises(ValueError):
        validate_references(Analysis.model_validate(analysis), snapshot)


def test_internal_service_auth_and_strict_input(snapshot):
    with TestClient(app) as http:
        assert http.get("/health").status_code == 200
        assert http.get("/v1/status").status_code == 401
        assert http.post("/v1/plan", json=snapshot.model_dump()).status_code == 401
        headers = {"X-Vigil-Service": "local-synthetic-mission-only"}
        result = http.post("/v1/plan", json=snapshot.model_dump(), headers=headers)
        assert result.status_code == 200
        assert result.json()["mode"] == "BASELINE"
        invalid = {**snapshot.model_dump(), "evidence": []}
        assert http.post("/v1/plan", json=invalid, headers=headers).status_code == 422
