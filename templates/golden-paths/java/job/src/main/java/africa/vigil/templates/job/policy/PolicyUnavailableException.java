// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy unavailable signal
package africa.vigil.templates.job.policy;

/** Warden could not decide; the job exits 3 without doing any work. */
public final class PolicyUnavailableException extends RuntimeException {

    public PolicyUnavailableException(String message) {
        super(message);
    }
}
