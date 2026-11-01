// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Fail-closed job Warden stub
package africa.vigil.templates.job.policy;

/** The stub that stands in until the P3 Warden binding exists. */
public final class UnavailableWarden implements WardenBinding {

    @Override
    public PolicyContext authorize(String tenantId, String purpose, String token) {
        throw new PolicyUnavailableException("the Warden binding is not wired yet");
    }
}
