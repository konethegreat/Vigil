# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Internal-only intelligence API
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
import hmac
import os
from contextlib import asynccontextmanager

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Request

from .contracts import PlanRequest
from .planner import Planner


def authorize(x_vigil_service: str = Header(default="")) -> None:
    expected = os.getenv("VIGIL_INTERNAL_TOKEN", "")
    if len(expected) < 16 or not hmac.compare_digest(expected, x_vigil_service):
        raise HTTPException(status_code=401, detail="Authenticated service context required")


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with httpx.AsyncClient(follow_redirects=False) as client:
        app.state.planner = Planner(client)
        yield


app = FastAPI(title="Vigil Mind", version="0.1.0", lifespan=lifespan, docs_url=None, redoc_url=None)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "vigil-mind"}


@app.get("/v1/status", dependencies=[Depends(authorize)])
async def status(request: Request):
    return await request.app.state.planner.status()


@app.post("/v1/plan", dependencies=[Depends(authorize)])
async def plan(body: PlanRequest, request: Request):
    return await request.app.state.planner.plan(body)
