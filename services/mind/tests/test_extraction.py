# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Source extraction boundaries
import hashlib
import json

import httpx
import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError

from vigil_mind.extraction import ExtractRequest, Proposal, extract
from vigil_mind.main import app
from vigil_mind.planner import Planner

TEXT = "  Municipal Pump 4 is located at Station North.\nThis is a synthetic inspection.\n"


@pytest.fixture(autouse=True)
def config(monkeypatch):
    monkeypatch.setenv("VIGIL_OLLAMA_URL", "http://localhost:11434")
    monkeypatch.setenv("VIGIL_REASONER_MODEL", "local-reasoner")
    monkeypatch.setenv("VIGIL_CHALLENGER_MODEL", "")
    monkeypatch.setenv("VIGIL_INTERNAL_TOKEN", "local-synthetic-mission-only")


@pytest.fixture
def source():
    return ExtractRequest(
        mission_id="VGL-TEST",
        evidence_id="SRC-TEST",
        evidence_version=1,
        original=TEXT,
        sha256=hashlib.sha256(TEXT.encode()).hexdigest(),
    )


@pytest.fixture
def proposal():
    return {
        "objects": [
            {
                "id": "o1",
                "object_type": "ASSET",
                "title": "Municipal Pump 4",
                "summary": "The source describes a municipal pump.",
                "quote": "Municipal Pump 4",
            },
            {
                "id": "o2",
                "object_type": "PLACE",
                "title": "Station North",
                "summary": "The source names this station.",
                "quote": "Station North",
            },
        ],
        "links": [
            {
                "id": "l1",
                "from_id": "o1",
                "to_id": "o2",
                "relation": "LOCATED_AT",
                "reason": "The inspection places the pump at the station.",
                "quote": "Municipal Pump 4 is located at Station North.",
            }
        ],
        "gaps": ["Independent confirmation has not been supplied."],
    }


def test_source_hash_preserves_whitespace(source):
    assert source.original == TEXT
    with pytest.raises(ValidationError):
        ExtractRequest.model_validate({**source.model_dump(), "original": TEXT.strip()})


@pytest.mark.parametrize("change", ["quote", "endpoint", "pair", "duplicate", "authority"])
def test_invalid_proposals_are_rejected(proposal, change):
    if change == "quote":
        proposal["objects"][0]["quote"] = "Invented pump"
    elif change == "endpoint":
        proposal["links"][0]["to_id"] = "missing"
    elif change == "pair":
        proposal["links"][0]["relation"] = "SUPPLIED_BY"
    elif change == "duplicate":
        proposal["objects"].append(proposal["objects"][0])
    else:
        proposal["links"][0]["assessment"] = "CORROBORATED"
    with pytest.raises(ValueError):
        Proposal.model_validate(proposal).check_quotes(TEXT)


async def test_grounded_reasoner_and_optional_challenger(source, proposal, monkeypatch):
    monkeypatch.setenv("VIGIL_CHALLENGER_MODEL", "local-reviewer")
    calls = []

    def respond(request):
        body = json.loads(request.content)
        calls.append(body)
        output = (
            proposal if body["model"] == "local-reasoner" else {"concerns": ["Confirm location."]}
        )
        return httpx.Response(200, json={"message": {"content": json.dumps(output)}})

    async with httpx.AsyncClient(transport=httpx.MockTransport(respond)) as client:
        result = await extract(Planner(client), source)
    assert result["proposal"] == proposal
    assert result["review"]["state"] == "COMPLETE"
    assert result["review"]["concerns"] == ["Confirm location."]
    assert len(calls) == 2
    assert "<untrusted_content" in calls[0]["messages"][1]["content"]
    assert "tools" not in calls[0]
    assert calls[0]["options"]["num_predict"] == 1800


async def test_challenger_failure_keeps_proposal_but_reports_unavailable(
    source, proposal, monkeypatch
):
    monkeypatch.setenv("VIGIL_CHALLENGER_MODEL", "local-reviewer")

    def respond(request):
        if json.loads(request.content)["model"] == "local-reviewer":
            return httpx.Response(500)
        return httpx.Response(200, json={"message": {"content": json.dumps(proposal)}})

    async with httpx.AsyncClient(transport=httpx.MockTransport(respond)) as client:
        result = await extract(Planner(client), source)
    assert result["proposal"] == proposal
    assert result["review"]["state"] == "UNAVAILABLE"
    assert result["warnings"]


def test_service_auth_hash_validation_and_unavailable_are_distinct(source, monkeypatch):
    monkeypatch.setenv("VIGIL_REASONER_MODEL", "")
    with TestClient(app) as client:
        assert client.post("/v1/extract", json=source.model_dump()).status_code == 401
        headers = {"X-Vigil-Service": "local-synthetic-mission-only"}
        assert (
            client.post("/v1/extract", json=source.model_dump(), headers=headers).status_code == 503
        )
        invalid = {**source.model_dump(), "sha256": "0" * 64}
        assert client.post("/v1/extract", json=invalid, headers=headers).status_code == 422


async def test_reasoner_failure_does_not_fabricate_a_proposal(source):
    async with httpx.AsyncClient(transport=httpx.MockTransport(lambda _: httpx.Response(503))) as c:
        with pytest.raises(httpx.HTTPError):
            await extract(Planner(c), source)
