// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-27 — Intake and selection boundaries
import { describe, expect, it } from "vitest";
import {
  decodeSource,
  retainLinks,
  type LinkProposal,
} from "../src/lib/extractions";

const bytes = (value: string) => new TextEncoder().encode(value).buffer;
describe("source intake", () => {
  it("preserves Unicode, whitespace, BOM and CRLF exactly", () => {
    const original = "\uFEFF  eThekwini — synthetic source\r\nSecond line.\r\n";
    expect(decodeSource("report.TXT", bytes(original))).toBe(original);
  });
  it("rejects binary, invalid UTF-8, empty and oversize inputs without truncation", () => {
    expect(() => decodeSource("report.pdf", bytes("Fictional report"))).toThrow(
      ".txt",
    );
    expect(() =>
      decodeSource("report.txt", new Uint8Array([0xff, 0xfe, 0xff]).buffer),
    ).toThrow("UTF-8");
    expect(() => decodeSource("report.txt", bytes("      "))).toThrow(
      "characters",
    );
    expect(() => decodeSource("report.txt", bytes("Source\0payload"))).toThrow(
      "NUL",
    );
    expect(() => decodeSource("report.md", bytes("a".repeat(16001)))).toThrow(
      "never truncated",
    );
    expect(() => decodeSource("report.md", bytes("a".repeat(64001)))).toThrow(
      "64 KB",
    );
  });
});
it("drops selected links when either supporting object is deselected", () => {
  const links = [
    { id: "l1", from_id: "o1", to_id: "o2" },
    { id: "l2", from_id: "o2", to_id: "o3" },
  ] as LinkProposal[];
  expect(retainLinks(links, ["o1", "o2"], ["l1", "l2"])).toEqual(["l1"]);
  expect(retainLinks(links, ["o2"], ["l1"])).toEqual([]);
});
