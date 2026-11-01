// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Vitest and coverage floors for the library template (P1 task 1.4)
import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    include: ["test/**/*.test.ts"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.ts"],
      // A barrel with only re-exports has no executable statements; the tests
      // import through it, so the modules it re-exports are still measured.
      exclude: ["src/index.ts"],
      reporter: ["text", "lcov"],
      // A library is small and fully exercised: hold the same 100 % floor Java uses.
      thresholds: {
        lines: 100,
        functions: 100,
        branches: 100,
        statements: 100,
      },
    },
  },
});
