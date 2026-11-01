// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Library settings tests (TDD)
import { describe, expect, it } from "vitest";

import { librarySettings } from "../src/index.js";

describe("librarySettings", () => {
  it("applies the documented batch size default", () => {
    expect(librarySettings({ namespace: "claims" })).toEqual({
      namespace: "claims",
      batchSize: 100,
    });
  });

  it("keeps a caller-supplied batch size", () => {
    expect(
      librarySettings({ namespace: "claims", batchSize: 25 }).batchSize,
    ).toBe(25);
  });

  it("rejects a blank namespace", () => {
    expect(() => librarySettings({ namespace: "   " })).toThrow(/namespace/);
  });

  it("rejects a batch size below one", () => {
    expect(() =>
      librarySettings({ namespace: "claims", batchSize: 0 }),
    ).toThrow(/batchSize/);
  });

  it("rejects a fractional batch size", () => {
    expect(() =>
      librarySettings({ namespace: "claims", batchSize: 1.5 }),
    ).toThrow(/batchSize/);
  });
});
