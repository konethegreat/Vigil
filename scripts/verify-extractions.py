# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Local source review acceptance
"""Exercise the running gateway, local model, review transaction and export with synthetic text."""

import argparse
import hashlib
import http.cookiejar
import json
import uuid
from urllib.error import HTTPError
from urllib.parse import urlparse
from urllib.request import HTTPCookieProcessor, Request, build_opener


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:3100")
    args = parser.parse_args()
    if urlparse(args.url).hostname not in {"127.0.0.1", "localhost"}:
        raise SystemExit("Only the local synthetic environment is supported.")
    base = args.url.rstrip("/")
    opener = build_opener(HTTPCookieProcessor(http.cookiejar.CookieJar()))
    checks = []

    def call(path, body=None, expected=200, key=None):
        request = Request(
            base + "/api/vigil/" + path,
            headers={
                "Content-Type": "application/json",
                "Origin": base,
                "Idempotency-Key": key or str(uuid.uuid4()),
            },
            data=json.dumps(body).encode() if body is not None else None,
        )
        try:
            response = opener.open(request, timeout=220)
        except HTTPError as error:
            response = error
        content = json.load(response)
        assert response.status == expected, (path, response.status, content)
        return content

    def check(label):
        checks.append(label)
        print("PASS " + label, flush=True)

    call("session", {"actor": "operations"})
    status = call("intelligence/status")
    assert status["state"] == "READY", (
        "A local reasoner is required for live extraction acceptance"
    )
    mission = call(
        "missions",
        {
            "domain": "INVESTIGATE",
            "title": "Acceptance check — quoted source review",
            "objective": "Verify exact source quotations and explicit analyst publication.",
            "location": "Fictional test district",
            "priority": "STANDARD",
        },
    )
    mid = mission["id"]
    original = "  SYNTHETIC: Municipal Pump 4 is located at Station North.\r\n"
    mission = call(
        f"missions/{mid}/commands",
        {
            "type": "add_evidence",
            "expected_version": mission["version"],
            "title": "Synthetic inspection source",
            "source": "Acceptance fixture",
            "kind": "DOCUMENT",
            "statement": "The fictional inspection reports a pump location.",
            "original": original,
        },
    )
    source = mission["evidence"][0]
    assert source["original"] == original
    assert source["sha256"] == hashlib.sha256(original.encode()).hexdigest()
    check("Whitespace, line endings and source text hash preserved")
    body = {
        "mission_id": mid,
        "evidence_id": source["id"],
        "expected_version": mission["version"],
    }
    key = str(uuid.uuid4())
    result = call("extractions", body, key=key)
    assert call("extractions", body, key=key) == result
    mission, run = result["mission"], result["run"]
    assert run["model"] == status["reasoner_model"]
    proposal = run["proposal"]
    assert len(proposal["objects"]) >= 2 and proposal["links"], (
        "Fixture requires a useful connected proposal"
    )
    for item in proposal["objects"] + proposal["links"]:
        assert item["quote"] in original
    assert run["decision"] is None
    assert not any(o["mission_id"] == mid for o in call("investigations")["objects"])
    assert call(f"missions/{mid}/extractions")["runs"][0]["id"] == run["id"]
    check(
        "Live local model generated quoted proposals without publishing records; retry is exact"
    )
    call("extractions", {**body, "expected_version": mission["version"]}, expected=409)
    rid = run["id"]
    review = {
        "expected_version": mission["version"],
        "object_ids": [o["id"] for o in proposal["objects"]],
        "link_ids": [l["id"] for l in proposal["links"]],
        "note": "Synthetic analyst review: exact quoted location retained as reported; independent confirmation pending.",
    }
    call(f"extractions/{rid}/review", {**review, "object_ids": []}, expected=400)
    call(
        f"extractions/{rid}/review", {**review, "tenant_id": "care-demo"}, expected=400
    )
    assert not any(o["mission_id"] == mid for o in call("investigations")["objects"])
    check(
        "Unknown authority and missing relationship endpoints are rejected atomically"
    )
    key = str(uuid.uuid4())
    saved = call(f"extractions/{rid}/review", review, key=key)
    assert call(f"extractions/{rid}/review", review, key=key) == saved
    call(f"extractions/{rid}/review", review, expected=409)
    decision = saved["run"]["decision"]
    graph = call("investigations")
    objects = [o for o in graph["objects"] if o["mission_id"] == mid]
    links = [l for l in graph["links"] if l["mission_id"] == mid]
    assert {o["id"] for o in objects} == set(decision["object_records"].values())
    assert {l["id"] for l in links} == set(decision["link_records"].values())
    assert all(
        l["assessment"] == "REPORTED" and l["original_sha256"] == source["sha256"]
        for l in links
    )
    check(
        "Selected records publish once with source provenance and reported assessment"
    )
    call("session", {"actor": "reviewer"})
    assert call(f"missions/{mid}/extractions")["runs"][0]["decision"] == decision
    call(f"extractions/{rid}/review", review, expected=403)
    exported = call(f"missions/{mid}/export")
    assert exported["extractions"][0]["proposal"] == proposal
    assert exported["extractions"][0]["decision"] == decision
    assert exported["audit"]["verified"] and exported["audit"]["event_count"] == 4
    check(
        "Read-only inspection and export retain original proposals, decisions and verified audit"
    )
    call("session", {"actor": "protection"})
    call(f"missions/{mid}/extractions", expected=404)
    call(f"extractions/{rid}/review", review, expected=404)
    assert not any(o["mission_id"] == mid for o in call("investigations")["objects"])
    check("Protection workspace cannot read or publish another tenant's proposals")
    print(
        json.dumps(
            {
                "passed": len(checks),
                "mission_id": mid,
                "extraction_id": rid,
                "model": run["model"],
                "review": run["review"]["state"],
                "objects": len(objects),
                "relationships": len(links),
                "audit_events": 4,
            },
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
