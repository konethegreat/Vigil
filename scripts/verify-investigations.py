# Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-26 - Local investigation HTTP acceptance
"""Verify the deployed local cockpit-to-database investigation workflow with synthetic records."""
import argparse
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
        headers = {"Content-Type": "application/json", "Origin": base,
                   "Idempotency-Key": key or str(uuid.uuid4())}
        request = Request(base + "/api/vigil/" + path, headers=headers,
                          data=json.dumps(body).encode() if body is not None else None)
        try:
            response = opener.open(request, timeout=30)
        except HTTPError as error:
            response = error
        content = json.load(response)
        assert response.status == expected, (path, response.status, content)
        return content

    def check(label):
        checks.append(label)
        print("PASS " + label, flush=True)

    call("session", {"actor": "operations"})
    initial = call("investigations")
    assert any(o["mission_id"] == "VGL-I001" for o in initial["objects"])
    assert any(link["mission_id"] == "VGL-I002" for link in initial["links"])
    check("Connected investigation fixtures survive forward migration")
    mission = call("missions", {"title": "Acceptance check — connected investigation", "domain": "INVESTIGATE",
                   "priority": "STANDARD", "location": "Synthetic verification district",
                   "objective": "Verify source-backed assertions and preserve unresolved evidence."})
    mid = mission["id"]
    mission = call(f"missions/{mid}/commands", {"type": "add_evidence", "expected_version": mission["version"],
                   "title": "Synthetic relationship source", "source": "Acceptance fixture", "kind": "DOCUMENT",
                   "statement": "A fictional inventory refers to a fictional storage site.",
                   "original": "SYNTHETIC: inventory entry references a site; confirmation pending."})
    source = mission["evidence"][0]

    def command(operation, **fields):
        nonlocal mission
        body = {"type": operation, "mission_id": mid, "expected_version": mission["version"], **fields}
        result = call("investigations/commands", body)
        mission = result["mission"]
        return result["record_id"]

    common = {"evidence_id": source["id"], "summary": "A clearly synthetic source-backed object."}
    asset = command("create_object", object_type="ASSET", title="Acceptance controller stock", **common)
    place = command("create_object", object_type="PLACE", title="Acceptance storage site", **common)
    body = {"type": "create_link", "mission_id": mid, "expected_version": mission["version"],
            "from_id": asset, "to_id": place, "relation": "LOCATED_AT", "evidence_id": source["id"],
            "assessment": "REPORTED", "reason": "The inventory reports this location; confirmation is pending."}
    key = str(uuid.uuid4())
    result = call("investigations/commands", body, key=key)
    assert call("investigations/commands", body, key=key) == result
    call("investigations/commands", {**body, "reason": "Changed retry contents."}, expected=409, key=key)
    call("investigations/commands", body, expected=409)
    mission = result["mission"]
    link_id = result["record_id"]
    check("Typed source-backed writes, exact retry and stale version handling")
    forged = {**body, "expected_version": mission["version"], "tenant_id": "care-demo"}
    call("investigations/commands", forged, expected=400)
    check("Client tenant forgery rejected")

    mission = call(f"missions/{mid}/commands", {"type": "review_evidence", "expected_version": mission["version"],
                   "evidence_id": source["id"], "status": "DISPUTED", "statement": "A conflicting source disputes the location.",
                   "note": "Synthetic discrepancy requires a further independent check."})

    def find_link():
        return next(link for link in call("investigations")["links"] if link["id"] == link_id)

    assert find_link()["provenance"]["needs_review"]
    command("review_link", link_id=link_id, expected_revision=1, assessment="DISPUTED",
            reason="The revised source disputes this location; retain the original claim in history.")
    link = find_link()
    assert not link["provenance"]["needs_review"] and link["revision"] == 2
    assert [r["assessment"] for r in link["history"]] == ["REPORTED", "DISPUTED"]
    assert link["original_sha256"] == source["sha256"]
    check("Source changes flag review; revision history and original hash persist")
    call("session", {"actor": "reviewer"})
    assert find_link()["revision"] == 2
    call("investigations/commands", {**body, "expected_version": mission["version"]}, expected=403)
    export = call(f"missions/{mid}/export")
    assert export["audit"]["verified"]
    check("Read-only review and verified mission audit export")
    call("session", {"actor": "protection"})
    protected = call("investigations")
    assert not any(o["id"] in {asset, place} for o in protected["objects"])
    assert not any(link["id"] == link_id for link in protected["links"])
    call(f"missions/{mid}", expected=404)
    check("Protection workspace cannot read operations objects, assertions or mission")
    call("session", {"actor": "operations"})
    assert find_link()["revision"] == 2
    print(json.dumps({"passed": len(checks), "mission_id": mid, "object_ids": [asset, place],
                      "link_id": link_id, "audit_events": export["audit"]["event_count"]}, indent=2))


if __name__ == "__main__":
    main()
