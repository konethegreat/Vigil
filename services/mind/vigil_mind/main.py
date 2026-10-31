# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Internal-only intelligence API
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
# Modified for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Source extraction
import hmac
import os
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import cast

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Request

from .contracts import PlanRequest
from .extraction import ExtractRequest, extract
from .planner import Planner


def authorize(x_vigil_service: str = Header(default="")) -> None:
    expected = os.getenv("VIGIL_INTERNAL_TOKEN", "")
    if len(expected) < 16 or not hmac.compare_digest(expected, x_vigil_service):
        raise HTTPException(status_code=401, detail="Authenticated service context required")


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    async with httpx.AsyncClient(follow_redirects=False) as client:
        app.state.planner = Planner(client)
        yield


app = FastAPI(title="Vigil Mind", version="0.1.0", lifespan=lifespan, docs_url=None, redoc_url=None)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok", "service": "vigil-mind"}


def planner(request: Request) -> Planner:
    return cast(Planner, request.app.state.planner)


@app.get("/v1/status", dependencies=[Depends(authorize)])
async def status(request: Request) -> dict[str, object]:
    return await planner(request).status()


@app.post("/v1/plan", dependencies=[Depends(authorize)])
async def plan(body: PlanRequest, request: Request) -> dict[str, object]:
    return await planner(request).plan(body)


@app.post("/v1/extract", dependencies=[Depends(authorize)])
async def extract_source(body: ExtractRequest, request: Request) -> dict[str, object]:
    try:
        return await extract(planner(request), body)
    except (httpx.HTTPError, ValueError, KeyError, TypeError) as exc:
        raise HTTPException(
            status_code=503,
            detail="Extraction was unavailable or failed source validation; no proposal was saved.",
        ) from exc
