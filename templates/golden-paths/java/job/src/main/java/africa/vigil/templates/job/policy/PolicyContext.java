// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Verified job policy context
package africa.vigil.templates.job.policy;

/** The tenant, purpose and signed token the job was authorized with. */
public record PolicyContext(String tenantId, String purpose, String signedToken) {

    public PolicyContext {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        if (signedToken == null || signedToken.isBlank()) {
            throw new IllegalArgumentException("signedToken must not be blank");
        }
    }

    @Override
    public String toString() {
        // Never let the token reach a log line or trace (BACKEND_GUIDE B5).
        return "PolicyContext[tenantId=" + tenantId + ", purpose=" + purpose + "]";
    }
}
