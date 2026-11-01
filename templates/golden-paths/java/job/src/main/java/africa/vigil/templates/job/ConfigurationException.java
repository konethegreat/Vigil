// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Invalid job configuration
package africa.vigil.templates.job;

/** The environment is missing or inconsistent; the job exits 2 without starting work. */
public final class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }
}
