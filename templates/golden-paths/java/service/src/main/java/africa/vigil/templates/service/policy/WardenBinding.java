// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Warden seam
package africa.vigil.templates.service.policy;

/**
 * The only way a route learns who is calling.
 *
 * <p>Until the P3 binding is wired, {@link UnavailableWarden} is the only implementation. It fails
 * closed: no bypass flag, no system context, no allow-all mode (INSTRUCTIONS §9.1).
 */
public interface WardenBinding {

    /** Whether Warden can answer policy questions right now. */
    boolean ready();

    /**
     * Authorizes one request.
     *
     * @throws PolicyUnavailableException when no decision can be made
     * @throws PolicyDeniedException when the decision is deny
     */
    PolicyContext authorize(String tenantId, String purpose, String token);
}
