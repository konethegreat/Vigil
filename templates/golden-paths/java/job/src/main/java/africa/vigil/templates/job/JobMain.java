// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job entry point
package africa.vigil.templates.job;

import africa.vigil.templates.job.policy.PolicyContext;
import africa.vigil.templates.job.policy.PolicyUnavailableException;
import africa.vigil.templates.job.policy.UnavailableWarden;
import africa.vigil.templates.job.policy.WardenBinding;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/** One-shot entry point with explicit exit codes. */
public final class JobMain {

    public static final int EXIT_OK = 0;
    public static final int EXIT_CONFIG = 2;
    public static final int EXIT_POLICY = 3;

    private static final String TRACER_NAME = "africa.vigil.templates.job";

    private JobMain() {}

    public static void main(String[] args) {
        Settings settings;
        try {
            settings = Settings.fromEnvironment(System.getenv());
        } catch (ConfigurationException e) {
            System.getLogger(JobMain.class.getName())
                    .log(System.Logger.Level.ERROR, "invalid configuration: {0}", e.getMessage());
            System.exit(EXIT_CONFIG);
            return;
        }
        System.exit(run(settings, new UnavailableWarden()));
    }

    /**
     * Runs one job with the supplied binding.
     *
     * <p>The provider is always shut down, so buffered spans are flushed on both paths.
     */
    public static int run(Settings settings, WardenBinding warden) {
        SdkTracerProvider provider = Telemetry.provider(settings);
        try {
            PolicyContext context;
            try {
                context = warden.authorize(
                        settings.tenantId(), settings.purpose(), settings.policyTokenRef());
            } catch (PolicyUnavailableException e) {
                return EXIT_POLICY;
            }
            return Execute.execute(settings, context, provider.get(TRACER_NAME));
        } finally {
            provider.close();
        }
    }
}
