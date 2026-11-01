// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Vitest and coverage floors for the service template (P1 task 1.4)
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["test/**/*.test.ts"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.ts"],
      // main.ts is the process entry: it wires the validated settings to the
      // server and owns signal handling, the same code the Java service leaves
      // outside its coverage floor.
      exclude: ["src/main.ts", "src/index.ts"],
      reporter: ["text", "lcov"],
      thresholds: {
        lines: 85,
        functions: 85,
        branches: 85,
        statements: 85,
        // The policy seam is the one module that must never be partially tested.
        "src/policy.ts": {
          lines: 100,
          functions: 100,
          branches: 100,
          statements: 100,
        },
      },
    },
  },
});
