<!-- Written by Kone Tshivhinda (Gemini 3.8 Flash) on 2026-10-16 — Repository security policy and disclosure guidelines -->
# Security Policy

Vigil is sovereign operational intelligence software designed to process sensitive institutional data in constrained, sovereign and air-gapped environments. We take the security, integrity, and privacy of our systems and users with the utmost seriousness.

## Supported Versions

Only the latest active release branches receive security patches and backports:

| Version / Branch | Supported | Notes |
| ---------------- | --------- | ----- |
| `main`           | :white_check_mark: | Production releases |
| `develop`        | :white_check_mark: | Active integration branch |
| `codex/*`        | :white_check_mark: | Authorized delivery increment branches |
| `< 0.1.0`        | :x:       | Older experimental tags and branches |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

To report a vulnerability responsibly:

1. **GitHub Private Vulnerability Reporting (Preferred):**
   Navigate to the [Security tab](https://github.com/konethegreat/Vigil/security) of the repository, click **Advisories**, and select **Report a vulnerability**. This creates a confidential advisory between you and the repository maintainers.

2. **Direct Confidential Email:**
   If GitHub Private Vulnerability Reporting is unavailable, email:
   **`erictshivhinda@gmail.com`**
   - Subject line: `[SECURITY] Vulnerability Report - Vigil`
   - Please include:
     - Component(s) affected (`core`, `warden`, `mind`, `cockpit`, `relay`, `field`)
     - Description of the potential vulnerability and impact
     - Proof-of-concept script, reproduction steps, or request trace
     - Any proposed mitigations or patches

### Response Timeline

- **Initial Acknowledgment:** Within **48 hours** of report receipt.
- **Triage & Severity Assessment:** Within **5 business days**, with CVSS scoring where applicable.
- **Remediation & Patching:** High and critical severity issues receive emergency patches on an accelerated timeline before public disclosure.
- **Responsible Disclosure:** We will coordinate a coordinated disclosure date with the reporter. Maintainers and reporters will be credited in the release notes and advisory (unless anonymity is requested).

## Core Security Guarantees & Constraints

All contributors and auditors should note Vigil's mandatory non-negotiables:

1. **Warden CBAC & RLS:** No service or client may read or write operational records without an authenticated `PolicyContext` validated by the Warden policy kernel.
2. **Untrusted Content Boundaries:** All untrusted external inputs (documents, scraped intelligence, raw sensor dumps) fed into AI components must be wrapped in strictly bounded XML blocks (`<untrusted_content>`) with extraction-only prompts to prevent prompt injection.
3. **No Secrets in Source:** Secret scanning and push protection are enforced on this repository. Never commit credentials, tokens, private keys, or `.env` files.
4. **Data Sovereignty & Air-Gap:** Vigil is architected to operate fully on-premises without mandatory egress to hyperscaler clouds. Model inference runs through local or sovereign private endpoints.
5. **POPIA / Privacy Safeguards:** Personal identifiers are protected under South African POPIA provisions (including Section 57 linkage lock and Section 71 human-in-the-loop review for automated consequential decisions). Synthetic or pseudonymized fixtures are mandatory for testing.
