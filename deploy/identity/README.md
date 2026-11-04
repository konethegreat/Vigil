<!-- Written by Kone Tshivhinda (Gemini 4 Argon) on 2026-11-04 - Identity and ReBAC deployment documentation (P3 task 3.4) -->
# Identity, OIDC, and ReBAC Configuration (`deploy/identity`)

This directory contains the in-boundary authentication and coarse authorization
definitions for Vigil (P3 task 3.4):

1. **Keycloak OIDC Realm ([`keycloak/vigil-realm.json`](keycloak/vigil-realm.json)):**
   - Enforces short-lived access tokens (`accessTokenLifespan = 300s`), TOTP
     (`HmacSHA256`) / WebAuthn MFA policies, and brute-force lockout protection.
   - Defines operational and privileged roles (`vigil-analyst`,
     `vigil-investigator`, `vigil-data-steward`, `vigil-dpo`,
     `vigil-security-admin`).
   - Provides the `vigil-warden-claims` OIDC client scope that maps
     `vigil_tenant`, `vigil_clearance`, `vigil_markings`, and
     `vigil_releasability` claims into the caller JWT consumed by Warden when
     building a [`PolicyContext`](../../services/warden/src/model.rs).
   - No client secrets or private keys are stored in git; runtime credentials
     are injected via OpenBao / Kubernetes Secrets at deployment time.

2. **OpenFGA ReBAC Model ([`openfga/model.fga`](openfga/model.fga)):**
   - Schema 1.1 relationship model covering `user`, `service_principal`,
     `organization`, `space`, `project`, `dataset`, `object_type`,
     `action_type`, and `case`.
   - OpenFGA gates coarse container membership (`can_read`, `can_write`,
     `can_query`, `can_submit`, `can_approve`, `can_read_case`). Fine-grained
     four-part label evaluation ($\lambda(R) = \langle M, C, REL, P \rangle$)
     and column redaction are always enforced by Warden (`services/warden`).
