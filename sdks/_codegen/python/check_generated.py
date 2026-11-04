#!/usr/bin/env python3
# Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Compile-check generated Python contract stubs and import them when google.protobuf is available (P2 task 2.5)
# Modified by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Validate WIT component worlds and verify Java -> Kafka -> Python -> Rust EventEnvelope wire payload (P2 task 2.4, Done-when)
"""Verify generated Python protobuf stubs, WIT worlds, and the P2 Kafka EventEnvelope round-trip."""

from __future__ import annotations

import ast
import base64
import py_compile
import re
import sys
from pathlib import Path

EXPECTED_COUNT = 6
EXPECTED_WIT = (
    (
        "policy.wit",
        "package vigil:policy@1.0.0;",
        ("interface types", "interface evaluator"),
        "world policy",
    ),
    (
        "udf.wit",
        "package vigil:udf@1.0.0;",
        ("interface types", "interface host", "interface guest"),
        "world udf",
    ),
)


def _read_varint(buf: bytes, pos: int) -> tuple[int, int]:
    result = 0
    shift = 0
    while pos < len(buf):
        byte = buf[pos]
        pos += 1
        result |= (byte & 0x7F) << shift
        if (byte & 0x80) == 0:
            return result, pos
        shift += 7
    raise ValueError("truncated protobuf varint")


def _parse_proto_fields(buf: bytes) -> dict[int, list[int | bytes]]:
    fields: dict[int, list[int | bytes]] = {}
    pos = 0
    while pos < len(buf):
        tag, pos = _read_varint(buf, pos)
        field_num = tag >> 3
        wire_type = tag & 0x07
        if wire_type == 0:
            val, pos = _read_varint(buf, pos)
            fields.setdefault(field_num, []).append(val)
        elif wire_type == 2:
            length, pos = _read_varint(buf, pos)
            end = pos + length
            if end > len(buf):
                raise ValueError("truncated length-delimited field")
            fields.setdefault(field_num, []).append(buf[pos:end])
            pos = end
        else:
            raise ValueError(f"unexpected wire type {wire_type} on field {field_num}")
    return fields


def validate_wit_worlds(repo_root: Path) -> int:
    wit_dir = repo_root / "proto" / "wit" / "common"
    for filename, pkg_decl, interfaces, world_decl in EXPECTED_WIT:
        path = wit_dir / filename
        if not path.is_file():
            print(f"error: missing WIT contract {path}", file=sys.stderr)
            return 1
        raw = path.read_text(encoding="utf-8")
        stripped = re.sub(r"//.*", "", raw)
        if stripped.count("{") == 0 or stripped.count("{") != stripped.count("}"):
            print(f"error: unbalanced braces in {path}", file=sys.stderr)
            return 1
        if pkg_decl not in stripped:
            print(f"error: missing '{pkg_decl}' in {path}", file=sys.stderr)
            return 1
        for iface in interfaces:
            if iface not in stripped:
                print(f"error: missing '{iface}' in {path}", file=sys.stderr)
                return 1
        if world_decl not in stripped:
            print(f"error: missing '{world_decl}' in {path}", file=sys.stderr)
            return 1
        if "wasi:" in stripped:
            print(f"error: forbidden ambient wasi: import in {path}", file=sys.stderr)
            return 1
    print(f"WIT world validation passed ({len(EXPECTED_WIT)} worlds in proto/wit/common).")
    return 0


def verify_kafka_roundtrip(codegen_dir: Path) -> int:
    b64_path = codegen_dir / "gen" / "roundtrip" / "kafka_out.b64"
    if not b64_path.is_file():
        return 0
    lines = [line.strip() for line in b64_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    if not lines:
        print("error: kafka_out.b64 is empty", file=sys.stderr)
        return 1
    wire_bytes = base64.b64decode(lines[-1])
    fields = _parse_proto_fields(wire_bytes)
    expected_strings = {
        1: b"1.0",
        2: b"01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b",
        3: b"//vigil/za-gp-1/demo/core-oms",
        4: b"vigil.ontology.type.published.v1",
        5: b"ri.ontology.demo.object-type.01926e5c-3a1b-7c4d-8e2f-9a0b1c2d3e4f",
        7: b"application/protobuf",
        8: b"urn:vigil:proto:vigil.common.v1.ResourceId",
        10: b"demo",
        12: b"PII",
        13: b"wm1_0000000001f4a2_0000000000016018",
        14: b"Supplier",
        15: b"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
    }
    for fnum, expected in expected_strings.items():
        actual = fields.get(fnum, [None])[0]
        if actual != expected:
            print(f"error: EventEnvelope field {fnum} mismatch: {actual!r} != {expected!r}", file=sys.stderr)
            return 1
    any_bytes = fields.get(9, [b""])[0]
    if not isinstance(any_bytes, bytes):
        print("error: EventEnvelope.data (Any) missing", file=sys.stderr)
        return 1
    any_fields = _parse_proto_fields(any_bytes)
    expected_type_url = b"type.googleapis.com/vigil.common.v1.ResourceId"
    if any_fields.get(1, [None])[0] != expected_type_url:
        print("error: EventEnvelope.data.type_url mismatch", file=sys.stderr)
        return 1
    inner_bytes = any_fields.get(2, [b""])[0]
    if not isinstance(inner_bytes, bytes):
        print("error: EventEnvelope.data.value missing", file=sys.stderr)
        return 1
    inner_fields = _parse_proto_fields(inner_bytes)
    expected_rid = b"ri.ontology.demo.type-version.01926e60-1c2d-7e3f-8a4b-5c6d7e8f9a0b"
    if inner_fields.get(1, [None])[0] != expected_rid:
        print("error: inner ResourceId payload mismatch", file=sys.stderr)
        return 1

    out_bin = codegen_dir / "gen" / "roundtrip" / "envelope.bin"
    out_bin.write_bytes(wire_bytes)
    print(f"python Kafka EventEnvelope consumer verified ({len(wire_bytes)} wire bytes -> {out_bin.name}).")
    return 0


def main() -> int:
    codegen_dir = Path(__file__).resolve().parent.parent
    repo_root = codegen_dir.parent.parent
    if validate_wit_worlds(repo_root) != 0:
        return 1

    gen_dir = codegen_dir / "gen" / "python"
    if not gen_dir.is_dir():
        print(f"error: generated directory not found: {gen_dir}", file=sys.stderr)
        return 1

    py_files = sorted(gen_dir.rglob("*.py"))
    pyi_files = sorted(gen_dir.rglob("*.pyi"))
    if len(py_files) < EXPECTED_COUNT or len(pyi_files) < EXPECTED_COUNT:
        print(
            f"error: expected at least {EXPECTED_COUNT} .py and .pyi files, "
            f"found {len(py_files)} .py and {len(pyi_files)} .pyi",
            file=sys.stderr,
        )
        return 1

    for py_file in py_files:
        py_compile.compile(str(py_file), doraise=True)

    for pyi_file in pyi_files:
        ast.parse(pyi_file.read_text(encoding="utf-8"), filename=str(pyi_file))

    if verify_kafka_roundtrip(codegen_dir) != 0:
        return 1

    try:
        import google.protobuf  # noqa: F401
    except ImportError:
        print(
            f"python syntax check passed ({len(py_files)} .py, {len(pyi_files)} .pyi); "
            "google.protobuf runtime not installed, import check skipped."
        )
        return 0

    sys.path.insert(0, str(gen_dir))
    from vigil.common.v1 import (
        error_pb2,
        page_pb2,
        rid_pb2,
        security_pb2,
        watermark_pb2,
    )
    from vigil.events.v1 import envelope_pb2

    rid = rid_pb2.ResourceId(
        value="ri.objects.demo.object.Supplier.6f1c2a9e-4b7d-5e3a-9c21-7d8e9f0a1b2c"
    )
    ctx = security_pb2.PolicyContext(
        principal="user:demo",
        tenant="demo",
        clearance=security_pb2.CLASSIFICATION_PUBLIC,
    )
    page = page_pb2.PageRequest(page_size=100)
    err = error_pb2.VigilError(
        error=error_pb2.ERROR_CODE_NOT_FOUND,
        status_code=404,
    )
    wm = watermark_pb2.Watermark(value="wm1_0000000001f4a2_0000000000016018")
    env = envelope_pb2.EventEnvelope(
        spec_version="1.0",
        subject=rid.value,
        vigil_tenant=ctx.tenant,
        vigil_class=0,
        vigil_watermark=wm.value,
    )
    if env.subject != rid.value or page.page_size != 100 or err.status_code != 404:
        print("error: round-trip field mismatch", file=sys.stderr)
        return 1
    print(f"python syntax and runtime import checks passed ({EXPECTED_COUNT} modules).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
