// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job Warden seam
package africa.vigil.templates.job.policy;

/** The only way a job learns which tenant it may act for. */
public interface WardenBinding {

    /**
     * Authorizes the job run.
     *
     * @throws PolicyUnavailableException when no decision can be made
     */
    PolicyContext authorize(String tenantId, String purpose, String token);
}
