#!/usr/bin/env python3
# Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Python binding and 551-case conformance runner for the Warden Security Kernel (P3 task 3.3)
"""Python binding over the compiled ``vigil_warden`` native shared library.

Executes the shared 551-case Warden conformance suite and verifies that every
decision emits a hash-chained W12 audit event.
"""

from __future__ import annotations

import ctypes
from dataclasses import dataclass
from pathlib import Path
import platform
import sys


@dataclass(frozen=True)
class ConformanceDecisionSummary:
    """Decoded status word returned by ``warden_eval_conformance_case``."""

    allow: bool
    external_http_status: int
    redaction_count: int
    obligation_count: int


def _locate_warden_library() -> Path:
    root = Path(__file__).resolve().parents[2]
    target_dirs = [root / "target" / "release", root / "target" / "debug"]
    sys_name = platform.system().lower()
    if "windows" in sys_name:
        names = ["vigil_warden.dll"]
    elif "darwin" in sys_name:
        names = ["libvigil_warden.dylib"]
    else:
        names = ["libvigil_warden.so"]

    for d in target_dirs:
        for name in names:
            candidate = d / name
            if candidate.is_file():
                return candidate
    msg = f"could not locate compiled vigil_warden library under {target_dirs}"
    raise FileNotFoundError(msg)


class WardenPythonBinding:
    """Thin C-ABI binding to the Rust Warden policy core."""

    def __init__(self, lib_path: Path | None = None) -> None:
        resolved = lib_path if lib_path is not None else _locate_warden_library()
        self._lib = ctypes.CDLL(str(resolved))
        self._lib.warden_conformance_count.argtypes = []
        self._lib.warden_conformance_count.restype = ctypes.c_uint32
        self._lib.warden_run_conformance_suite.argtypes = []
        self._lib.warden_run_conformance_suite.restype = ctypes.c_uint32
        self._lib.warden_eval_conformance_case.argtypes = [ctypes.c_uint32]
        self._lib.warden_eval_conformance_case.restype = ctypes.c_uint32

    def conformance_count(self) -> int:
        """Returns the number of vectors in the shared conformance suite."""
        return int(self._lib.warden_conformance_count())

    def run_conformance_suite(self) -> int:
        """Runs all conformance cases plus W12 audit-chain verification."""
        return int(self._lib.warden_run_conformance_suite())

    def eval_conformance_case(self, case_index: int) -> ConformanceDecisionSummary:
        """Evaluates a single conformance vector by index."""
        packed = int(self._lib.warden_eval_conformance_case(case_index))
        if (packed & (1 << 31)) != 0:
            msg = f"conformance case_index {case_index} out of bounds"
            raise IndexError(msg)
        allow = bool(packed & 1)
        http_status = (packed >> 1) & 0x7FFF
        redactions = (packed >> 16) & 0xFF
        obligations = (packed >> 24) & 0x7F
        return ConformanceDecisionSummary(
            allow=allow,
            external_http_status=http_status,
            redaction_count=redactions,
            obligation_count=obligations,
        )


def main() -> int:
    binding = WardenPythonBinding()
    total = binding.conformance_count()
    if total < 500:
        print(f"FAIL: expected >= 500 conformance cases, got {total}", file=sys.stderr)
        return 1
    verified = binding.run_conformance_suite()
    if verified != total:
        print(
            f"FAIL: warden_run_conformance_suite returned {verified}, expected {total}",
            file=sys.stderr,
        )
        return 1
    for idx in range(total):
        summary = binding.eval_conformance_case(idx)
        if summary.external_http_status not in (200, 403, 404):
            print(f"FAIL: unexpected HTTP status on case {idx}: {summary}", file=sys.stderr)
            return 1
        if summary.obligation_count < 1:
            print(f"FAIL: case {idx} missing mandatory W12 AUDIT_READ obligation", file=sys.stderr)
            return 1
    print(f"PASS [python-binding]: verified {verified}/{total} Warden conformance cases + W12 audit chain")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
