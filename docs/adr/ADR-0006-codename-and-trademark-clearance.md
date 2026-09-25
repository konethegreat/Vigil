<!-- Written by Kone Tshivhinda (Claude Opus 5.5) on 2026-09-25 — ADR-0006 codename and trademark clearance (P0) -->

# ADR-0006 — "Vigil" Is a Codename Until Trademark Clearance

**Status:** Proposed (P0), to be accepted at founder sign-off (P0 task 0.7) · **Date:** 2026-09-25

## Context
Research on 24–25 Sep 2026 ([appendix 07 §6](../research/appendix/07-funding-talent-gtm.md)) found:
- **vigil.co.za has been registered to ALTRON TMT (Pty) Ltd since 30 June 2005.**
- **3xLOGIC sells VIGIL**, a video-management system with face and plate recognition, into the security market Vigil targets.
- The USPTO lists about 500 "VIGIL" marks, 340 of them in Class 9.
- vigil.ai is parked for sale, and vigil.africa is registered.
- The CIPC register was not searched: it needs a login.

## Options
1. **Brand as "Vigil" now.**
   - Against: known conflicts (Altron's domain, 3xLOGIC's VIGIL) in the target market.
2. **Pick a new name now, without a search.**
   - Against: repeats the same risk.
3. **Keep "Vigil" as a codename and clear a name before public use (chosen).**

## Decision
1. "Vigil" is the **internal codename**. Code identifiers use `vigil` (Java `vigil.*`, crates `vigil-*`, npm `@vigil/*`) and **stay unchanged** if the public brand changes.
2. Before any public use or branding spend:
   - MB's legal practice searches CIPC and the WIPO Global Brand Database in Nice classes 9, 38, 42 and 45;
   - the team speaks to Altron (also a potential SI partner).
3. Prefer a **coined, distinctive company name**, ideally African-language and checked by native speakers, with a Madrid Protocol filing. Keep "Vigil" only as a cleared product-line or composite mark.
4. Register .africa, .co.za, .ai and .com together once a name is cleared.

## Consequences
The rename risk is contained to docs and marketing. Code and infrastructure don't depend on the public brand.
