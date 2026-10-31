<!-- Written by Kone Tshivhinda (OpenAI Codex GPT-6) on 2026-09-25 - Cockpit manifest attribution -->
<!-- Modified by Kone Tshivhinda (Gemini 3.8 Flash) on 2026-10-16 — Declared Apache-2.0 license in cockpit package manifest -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — Raised Next.js to the patched 16.4.3 release -->
<!-- Modified by Kone Tshivhinda (Qoder) on 2026-10-31 — Added the libvips LGPL notice required by ADR-0003 -->

# Manifest attribution

`package.json`, `tsconfig.json`, and `src/app/icon.svg` were authored by OpenAI Codex (GPT-6) for Kone Tshivhinda on 2026-09-25. On 2026-10-16, the Apache-2.0 license was declared in `package.json`. The TypeScript configuration also incorporates Next.js's generated type include paths. Next.js generated `AGENTS.md`, `CLAUDE.md`, and the ignored `next-env.d.ts`; these are project instruction/type files, not installed assistant applications.

## Third-party notices

`sharp` bundles an unmodified [libvips](https://www.libvips.org/) binary through the `@img/sharp-*` packages, licensed under LGPL-3.0-or-later. It is dynamically linked and not modified; its source is available from the `libvips` project and its npm distribution. `sharp` itself is Apache-2.0. This notice is required by ADR-0003 for allow-listed LGPL components.

