// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Caller-supplied library settings
package africa.vigil.templates.library;

/**
 * Configuration passed in by the caller. The library never reads the environment.
 *
 * @param namespace span-safe label for the work being summarised
 * @param batchSize items processed per unit of work
 */
public record LibrarySettings(String namespace, int batchSize) {

    public LibrarySettings {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
    }
}
