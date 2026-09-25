// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Server boundary tests
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
import { defineConfig } from "vitest/config";
export default defineConfig({
  test: {
    environment: "node",
    include: ["tests/**/*.test.ts"],
    restoreMocks: true,
  },
});
