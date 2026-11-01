// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy unavailable signal
package africa.vigil.templates.service.policy;

/** Warden could not decide. Routes answer 503 rather than guessing. */
public final class PolicyUnavailableException extends RuntimeException {

    public PolicyUnavailableException(String message) {
        super(message);
    }
}
