# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — FastAPI application factory
"""Application factory, health probes and one guarded example route.

Run it with the factory form, because the app needs validated settings:

    uvicorn vigil_service_template.main:create_app --factory
"""

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Annotated

from fastapi import Depends, FastAPI
from fastapi.responses import JSONResponse

from .config import Settings
from .deps import get_policy_context
from .policy import PolicyContext, UnavailableWarden, WardenBinding
from .telemetry import configure_telemetry, instrument_app


async def example(
    ctx: Annotated[PolicyContext, Depends(get_policy_context)],
) -> dict[str, str]:
    """Replace this with the real contract; keep the policy dependency."""
    return {"tenant_id": ctx.tenant_id, "purpose": ctx.purpose}


def create_app(settings: Settings, warden: WardenBinding | None = None) -> FastAPI:
    """Build the app. Tests pass a binding, production wires the P3 binding."""
    binding: WardenBinding = warden if warden is not None else UnavailableWarden()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        app.state.started = False
        app.state.warden = binding
        provider = configure_telemetry(settings)
        if provider is not None:
            instrument_app(app)
        logging.basicConfig(level=settings.log_level)
        app.state.started = True
        try:
            yield
        finally:
            app.state.started = False
            if provider is not None:
                provider.shutdown()

    app = FastAPI(
        title=settings.service_name,
        version="0.1.0",
        lifespan=lifespan,
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )

    @app.get("/healthz/live")
    async def live() -> dict[str, str]:
        return {"status": "ok"}

    @app.get("/healthz/startup")
    async def startup() -> JSONResponse:
        if app.state.started:
            return JSONResponse({"status": "ok"})
        return JSONResponse({"status": "starting"}, status_code=503)

    @app.get("/healthz/ready")
    async def ready() -> JSONResponse:
        if app.state.started and binding.ready():
            return JSONResponse({"status": "ready"})
        return JSONResponse({"status": "not_ready"}, status_code=503)

    app.add_api_route("/v1/example", example, methods=["GET"])
    return app
