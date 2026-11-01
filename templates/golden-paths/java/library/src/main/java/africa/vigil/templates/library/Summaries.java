// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Span-carrying summary helper
package africa.vigil.templates.library;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.util.List;

/** Summarises a batch while recording only tenant and purpose on the span. */
public final class Summaries {

    private static final AttributeKey<String> TENANT = AttributeKey.stringKey("vigil.tenant");
    private static final AttributeKey<String> PURPOSE = AttributeKey.stringKey("vigil.purpose");

    private Summaries() {}

    /**
     * Counts the items and reports whether they exceed the batch size.
     *
     * <p>The tracer is supplied by the caller. This class never configures a global provider,
     * and it records no item content (BACKEND_GUIDE B5).
     */
    public static Summary summarise(
            LibrarySettings settings, PolicyContext context, List<String> items, Tracer tracer) {
        Span span = tracer.spanBuilder("vigil.library.summarise").startSpan();
        try {
            span.setAttribute(TENANT, context.tenantId());
            span.setAttribute(PURPOSE, context.purpose());
            return new Summary(
                    settings.namespace(), items.size(), items.size() > settings.batchSize());
        } finally {
            span.end();
        }
    }
}
