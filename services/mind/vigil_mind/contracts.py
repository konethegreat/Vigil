# Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Validated model boundary
# Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Evidence(StrictModel):
    id: str = Field(min_length=3, max_length=60)
    title: str = Field(min_length=3, max_length=160)
    statement: str = Field(min_length=5, max_length=2000)
    status: Literal["SUPPORTED", "DISPUTED", "UNVERIFIED"]
    sha256: str = Field(pattern=r"^[a-f0-9]{64}$")


class PlanRequest(StrictModel):
    mission_id: str = Field(min_length=3, max_length=60)
    domain: Literal["PROTECT", "LIFELINES", "RESPONSE", "INTEGRITY", "INVESTIGATE"]
    objective: str = Field(min_length=10, max_length=2000)
    evidence_version: int = Field(ge=0)
    evidence: list[Evidence] = Field(min_length=1, max_length=40)


class Hypothesis(StrictModel):
    title: str = Field(min_length=5, max_length=180)
    assessment: Literal["PLAUSIBLE", "DISPUTED", "INSUFFICIENT_EVIDENCE"]
    explanation: str = Field(min_length=5, max_length=1600)
    evidence_ids: list[str] = Field(min_length=1, max_length=12)


class Analysis(StrictModel):
    summary: str = Field(min_length=10, max_length=2400)
    hypotheses: list[Hypothesis] = Field(min_length=1, max_length=5)
    gaps: list[str] = Field(min_length=1, max_length=8)
    next_steps: list[str] = Field(min_length=1, max_length=8)


class Challenge(StrictModel):
    concerns: list[Annotated[str, Field(min_length=3, max_length=1600)]] = Field(max_length=8)


def validate_references(analysis: Analysis, request: PlanRequest) -> Analysis:
    source_ids = {item.id for item in request.evidence}
    for hypothesis in analysis.hypotheses:
        if any(source_id not in source_ids for source_id in hypothesis.evidence_ids):
            raise ValueError("Model referenced a source outside this mission snapshot")
    if any(len(item) > 1600 for item in [*analysis.gaps, *analysis.next_steps]):
        raise ValueError("Model output exceeds the bounded analysis contract")
    return analysis
