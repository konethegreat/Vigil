// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Summary result
package africa.vigil.templates.library;

/**
 * The result of one summarise call.
 *
 * @param namespace the caller-supplied label
 * @param count total items seen, including items beyond the batch size
 * @param truncated whether the input was larger than the configured batch size
 */
public record Summary(String namespace, int count, boolean truncated) {}
