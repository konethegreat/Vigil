#!/usr/bin/env python3
# Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Compile-check generated Python contract stubs and import them when google.protobuf is available (P2 task 2.5)
"""Verify generated Python protobuf stubs and .pyi type stubs under sdks/_codegen/gen/python."""

from __future__ import annotations

import ast
import importlib
import py_compile
import sys
from pathlib import Path

MODULES = (
    "vigil.common.v1.rid_pb2",
    "vigil.common.v1.security_pb2",
    "vigil.common.v1.page_pb2",
    "vigil.common.v1.error_pb2",
    "vigil.common.v1.watermark_pb2",
    "vigil.events.v1.envelope_pb2",
)


def main() -> int:
    gen_dir = Path(__file__).resolve().parent.parent / "gen" / "python"
    if not gen_dir.is_dir():
        print(f"error: generated directory not found: {gen_dir}", file=sys.stderr)
        return 1

    py_files = sorted(gen_dir.rglob("*.py"))
    pyi_files = sorted(gen_dir.rglob("*.pyi"))
    if len(py_files) < len(MODULES) or len(pyi_files) < len(MODULES):
        print(
            f"error: expected at least {len(MODULES)} .py and .pyi files, "
            f"found {len(py_files)} .py and {len(pyi_files)} .pyi",
            file=sys.stderr,
        )
        return 1

    for py_file in py_files:
        py_compile.compile(str(py_file), doraise=True)

    for pyi_file in pyi_files:
        ast.parse(pyi_file.read_text(encoding="utf-8"), filename=str(pyi_file))

    try:
        import google.protobuf  # noqa: F401
    except ImportError:
        print(
            f"python syntax check passed ({len(py_files)} .py, {len(pyi_files)} .pyi); "
            "google.protobuf runtime not installed, import check skipped."
        )
        return 0

    sys.path.insert(0, str(gen_dir))
    for mod_name in MODULES:
        importlib.import_module(mod_name)

    from vigil.common.v1 import rid_pb2, security_pb2
    from vigil.events.v1 import envelope_pb2

    rid = rid_pb2.ResourceId(
        value="ri.objects.demo.object.Supplier.6f1c2a9e-4b7d-5e3a-9c21-7d8e9f0a1b2c"
    )
    ctx = security_pb2.PolicyContext(
        principal="user:demo",
        tenant="demo",
        clearance=security_pb2.CLASSIFICATION_PUBLIC,
    )
    env = envelope_pb2.EventEnvelope(
        spec_version="1.0",
        subject=rid.value,
        vigil_tenant=ctx.tenant,
        vigil_class=0,
    )
    if env.subject != rid.value:
        print("error: round-trip subject mismatch", file=sys.stderr)
        return 1
    print(f"python syntax and runtime import checks passed ({len(MODULES)} modules).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
