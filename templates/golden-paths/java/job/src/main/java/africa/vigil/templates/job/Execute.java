// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job work unit
package africa.vigil.templates.job;

import africa.vigil.templates.job.policy.PolicyContext;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/** The unit of work. Replace the body with the real job; keep the policy context parameter. */
public final class Execute {

    private static final Logger LOG = System.getLogger(Execute.class.getName());
    private static final AttributeKey<String> TENANT = AttributeKey.stringKey("vigil.tenant");
    private static final AttributeKey<String> PURPOSE = AttributeKey.stringKey("vigil.purpose");

    private Execute() {}

    public static int execute(Settings settings, PolicyContext context, Tracer tracer) {
        Span span = tracer.spanBuilder("vigil.job.execute").startSpan();
        try {
            span.setAttribute(TENANT, context.tenantId());
            span.setAttribute(PURPOSE, context.purpose());
            // Synthetic work: report the configured batch size, never any payload content.
            LOG.log(Level.INFO, "processed a batch of {0} items", settings.batchSize());
            return JobMain.EXIT_OK;
        } finally {
            span.end();
        }
    }
}
