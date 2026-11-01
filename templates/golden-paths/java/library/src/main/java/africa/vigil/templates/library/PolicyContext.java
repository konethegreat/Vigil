// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Verified request policy context
package africa.vigil.templates.library;

/**
 * The tenant and purpose a caller has already verified with Warden.
 *
 * <p>A library never constructs this from its own environment: the caller owns the policy
 * decision and passes the result in explicitly (BACKEND_GUIDE §9.1).
 */
public record PolicyContext(String tenantId, String purpose) {

    public PolicyContext {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
    }
}
