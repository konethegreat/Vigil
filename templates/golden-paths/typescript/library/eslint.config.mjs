// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — ESLint flat config for the library template (P1 task 1.4)
import js from "@eslint/js";
import { defineConfig, globalIgnores } from "eslint/config";
import tseslint from "typescript-eslint";

export default defineConfig(
  globalIgnores(["dist/", "coverage/", "node_modules/"]),
  js.configs.recommended,
  {
    files: ["src/**/*.ts", "test/**/*.ts"],
    extends: [tseslint.configs.strictTypeChecked],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
  },
  {
    // Build and tool config files sit outside the checked tsconfig project.
    files: ["*.config.ts", "*.config.mjs"],
    extends: [tseslint.configs.disableTypeChecked],
  },
);
