# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Optional local inference acceptance check
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
# Modified by Kone Tshivhinda (Qoder) on 2026-10-31 - CI format
"""Run from services/mind with uv run python scripts/check_local_models.py.

Uses only models explicitly named in environment variables and already installed
in Ollama. Never downloads weights or invokes hosted providers.
"""

import asyncio
import json
import os
import sys
import time
from pathlib import Path

import httpx

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from vigil_mind.contracts import PlanRequest  # noqa: E402
from vigil_mind.planner import Planner  # noqa: E402


async def main():
    if not os.getenv("VIGIL_REASONER_MODEL"):
        raise SystemExit("Set VIGIL_REASONER_MODEL to an already installed local model.")
    async with httpx.AsyncClient(follow_redirects=False) as client:
        planner = Planner(client)
        status = await planner.status()
        if status["state"] != "READY":
            raise SystemExit(json.dumps(status))
        started = time.monotonic()
        result = await planner.plan(
            PlanRequest(
                mission_id="VGL-MODEL-CHECK",
                domain="LIFELINES",
                evidence_version=1,
                objective="Restore a fictional water pump after validating the inspection record.",
                evidence=[
                    {
                        "id": "SRC-CHECK",
                        "title": "Synthetic pump inspection",
                        "statement": "A fictional maintenance team reports a failed pump controller. "
                        "Replacement availability is not confirmed.",
                        "status": "UNVERIFIED",
                        "sha256": "a" * 64,
                    }
                ],
            )
        )
        print(
            json.dumps(
                {
                    "mode": result["mode"],
                    "model": result["model"],
                    "review": result["review"],
                    "elapsed_seconds": round(time.monotonic() - started, 1),
                    "warnings": result["warnings"],
                    "summary": result["summary"],
                    "citation_check": result["citation_check"],
                },
                indent=2,
            )
        )
        if result["mode"] != "LOCAL_MODEL":
            raise SystemExit(1)


asyncio.run(main())
