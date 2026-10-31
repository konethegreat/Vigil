# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Policy context dependency
"""The auth to policy-context dependency (BACKEND_GUIDE §2.4)."""

from typing import Annotated, cast

from fastapi import Depends, Header, HTTPException, Request

from .policy import PolicyContext, PolicyDenied, PolicyUnavailable, WardenBinding


def get_warden(request: Request) -> WardenBinding:
    return cast(WardenBinding, request.app.state.warden)


def get_policy_context(
    warden: Annotated[WardenBinding, Depends(get_warden)],
    x_vigil_tenant: str = Header(default=""),
    x_vigil_purpose: str = Header(default=""),
    x_vigil_policy_token: str = Header(default=""),
) -> PolicyContext:
    """Build the request context through Warden, never by hand.

    Error details never echo request input (BACKEND_GUIDE §2.6).
    """
    if not (x_vigil_tenant and x_vigil_purpose and x_vigil_policy_token):
        raise HTTPException(status_code=400, detail="tenant, purpose and policy token are required")
    try:
        return warden.authorize(
            tenant_id=x_vigil_tenant,
            purpose=x_vigil_purpose,
            token=x_vigil_policy_token,
        )
    except PolicyUnavailable as exc:
        raise HTTPException(status_code=503, detail="policy evaluation unavailable") from exc
    except PolicyDenied as exc:
        raise HTTPException(status_code=403, detail="policy denied") from exc
