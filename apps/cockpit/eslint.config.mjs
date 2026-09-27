// Written for Kone Tshivhinda by OpenAI Codex (GPT-6) on 2026-09-25 — Cockpit static analysis
// Modified by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Recovery verification
import { defineConfig } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTypescript from "eslint-config-next/typescript";
export default defineConfig([
  ...nextVitals,
  ...nextTypescript,
  {
    rules: {
      // Synchronously clear the previous tenant's view before starting a new scoped request.
      "react-hooks/set-state-in-effect": "off",
    },
  },
]);
