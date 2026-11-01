// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy denied signal
package africa.vigil.templates.service.policy;

/** Warden denied the request. Routes answer 403 without echoing request input. */
public final class PolicyDeniedException extends RuntimeException {

    public PolicyDeniedException(String message) {
        super(message);
    }
}
