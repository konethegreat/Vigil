# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Local HTTP acceptance workflow
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
"""Exercise the running cockpit, BFF, Java core, Mind and PostgreSQL together.

Creates a clearly synthetic mission in the local operations workspace. No real
messages, external datasets, model downloads or paid services are invoked.
"""
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
        raise SystemExit("This verification is restricted to the local synthetic environment.")
    base = args.url.rstrip("/")
    opener = build_opener(HTTPCookieProcessor(http.cookiejar.CookieJar()))
    passed = []

    def call(path, body=None, *, key=None, expected=200, origin=None):
        headers = {"Content-Type": "application/json"}
        if body is not None:
            headers["Origin"] = origin or base
            headers["Idempotency-Key"] = key or str(uuid.uuid4())
        request = Request(base + "/api/vigil/" + path,
                          data=json.dumps(body).encode() if body is not None else None,
                          headers=headers)
        try:
            response = opener.open(request, timeout=230)
        except HTTPError as error:
            response = error
        content = json.load(response)
        assert response.status == expected, (path, response.status, content)
        return content

    def check(label):
        passed.append(label)
        print("PASS " + label, flush=True)

    initial = call("missions")
    assert initial["identity"]["actor"] == "operations"
    assert all(mission["domain"] != "PROTECT" for mission in initial["missions"])
    check("Session bootstrap and operations workspace")
    call("session", {"actor": "protection"}, expected=403, origin="https://untrusted.example")
    check("Cross-origin mutation rejected")
    call("missions/VGL-P018", expected=404)
    check("Protection mission invisible to operations")
    body = {"title": "Acceptance check — a service restored", "domain": "LIFELINES",
            "priority": "STANDARD", "location": "Synthetic verification area",
            "objective": "Complete the fictional service restoration and verify every handoff."}
    key = str(uuid.uuid4())
    mission = call("missions", body, key=key)
    assert call("missions", body, key=key) == mission
    call("missions", {**body, "title": "Changed retry"}, key=key, expected=409)
    check("Create is idempotent; changed retry rejected")
    mission_id = mission["id"]

    def command(operation, *, expected=200, **fields):
        nonlocal mission
        result = call(f"missions/{mission_id}/commands",
                      {"type": operation, "expected_version": mission["version"], **fields},
                      expected=expected)
        if expected == 200:
            mission = result
        return result

    command("generate_plan", expected=409)
    command("add_evidence", title="Synthetic field inspection", source="Verification fixture",
            kind="FIELD_REPORT", statement="The fictional service interruption needs repair.",
            original="SYNTHETIC RECORD: A test custodian reported a service interruption.")
    source = mission["evidence"][0]
    plan_request = {"type": "generate_plan", "expected_version": mission["version"]}
    plan_key = str(uuid.uuid4())
    mission = call(f"missions/{mission_id}/commands", plan_request, key=plan_key)
    assert call(f"missions/{mission_id}/commands", plan_request, key=plan_key) == mission
    assert mission["plan"]["mode"] in {"BASELINE", "LOCAL_MODEL"}
    check("Java-to-Mind plan generation and plan retry")
    old_version = mission["version"]
    command("review_evidence", evidence_id=source["id"], status="SUPPORTED",
            statement="The fictional inspection was confirmed by the test custodian.",
            note="A synthetic confirmation was supplied.")
    assert mission["evidence"][0]["sha256"] == source["sha256"]
    assert mission["evidence"][0]["original"] == source["original"]
    assert len(mission["evidence"][0]["history"]) == 1
    assert mission["plan"]["evidence_version"] != mission["evidence_version"]
    command("transition_task", task_id=mission["tasks"][0]["id"], state="AUTHORISED",
            receipt="Synthetic approval", expected=409)
    call(f"missions/{mission_id}/commands", {"type": "generate_plan", "expected_version": old_version}, expected=409)
    check("Original preserved; stale plan and concurrent edit rejected")
    command("generate_plan")
    command("record_outcome", summary="Claiming success prematurely.",
            verification="No task results yet.", expected=409)
    command("transition_task", task_id=mission["tasks"][0]["id"], state="CONFIRMED",
            receipt="Skipping acceptance is forbidden.", expected=409)
    check("Closure and task lifecycle gates enforced")
    call("session", {"actor": "reviewer"})
    call(f"missions/{mission_id}")
    command("generate_plan", expected=403)
    check("Reviewer reads but cannot mutate")
    call("session", {"actor": "protection"})
    protection = call("missions")
    assert all(m["domain"] == "PROTECT" for m in protection["missions"])
    call(f"missions/{mission_id}", expected=404)
    check("Switching identities does not leak the previous workspace")
    call("session", {"actor": "operations"})
    for task in mission["tasks"]:
        for state in ["AUTHORISED", "SENT", "ACCEPTED", "IN_PROGRESS", "CONFIRMED"]:
            command("transition_task", task_id=task["id"], state=state,
                    receipt=f"SYNTHETIC {state} receipt confirmed by the test custodian.")
    command("record_outcome", summary="The fictional service was restored and every result confirmed.",
            verification="The synthetic custodian reviewed all three task receipts and source records.")
    assert mission["status"] == "COMPLETED"
    check("Complete evidence-to-outcome mission")
    command("generate_plan", expected=409)
    exported = call(f"missions/{mission_id}/export")
    assert exported["audit"]["verified"] is True
    assert exported["mission"] == mission
    assert len(exported["audit"]["events"]) == 21
    check("Completed record is preserved; exported event chain verifies")
    print(json.dumps({"passed": len(passed), "mission_id": mission_id,
                      "plan_mode": mission["plan"]["mode"],
                      "audit_events": len(exported["audit"]["events"])}, indent=2))


if __name__ == "__main__":
    main()
