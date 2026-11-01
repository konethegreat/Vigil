// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job run tests
package africa.vigil.templates.job;

import static org.assertj.core.api.Assertions.assertThat;

import africa.vigil.templates.job.policy.PolicyContext;
import africa.vigil.templates.job.policy.UnavailableWarden;
import africa.vigil.templates.job.policy.WardenBinding;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobRunTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider =
            SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
    private final Tracer tracer = provider.get("test");

    @AfterEach
    void shutdown() {
        provider.close();
    }

    @BeforeEach
    void resetSpans() {
        exporter.reset();
    }

    private static Settings settings() {
        return new Settings(
                "tenant-a", "batch-summary", "http://warden.invalid:8080", "kms://vigil/test/job#1", 10, null);
    }

    @Test
    void returnsPolicyExitCodeWhileTheStubIsInPlace() {
        int exit = JobMain.run(settings(), new UnavailableWarden());

        assertThat(exit).isEqualTo(JobMain.EXIT_POLICY);
    }

    @Test
    void returnsZeroWhenAuthorized() {
        WardenBinding allowing = new WardenBinding() {
            @Override
            public PolicyContext authorize(String tenantId, String purpose, String token) {
                return new PolicyContext(tenantId, purpose, token);
            }
        };

        int exit = JobMain.run(settings(), allowing);

        assertThat(exit).isEqualTo(JobMain.EXIT_OK);
    }

    @Test
    void executeRecordsOnlyTenantAndPurpose() {
        PolicyContext context = new PolicyContext("tenant-a", "batch-summary", "synthetic-token");

        int exit = Execute.execute(settings(), context, tracer);

        assertThat(exit).isEqualTo(JobMain.EXIT_OK);
        assertThat(exporter.getFinishedSpanItems()).singleElement().satisfies(span -> {
            assertThat(span.getName()).isEqualTo("vigil.job.execute");
            AttributeKey<String> tenant = AttributeKey.stringKey("vigil.tenant");
            AttributeKey<String> purpose = AttributeKey.stringKey("vigil.purpose");
            assertThat(span.getAttributes().asMap()).containsOnlyKeys(tenant, purpose);
            assertThat(span.getAttributes().get(tenant)).isEqualTo("tenant-a");
            assertThat(span.getAttributes().get(purpose)).isEqualTo("batch-summary");
        });
    }

    @Test
    void executeNeverRecordsTheTokenOrBatchContents() {
        PolicyContext context = new PolicyContext("tenant-a", "batch-summary", "synthetic-token");

        Execute.execute(settings(), context, tracer);

        assertThat(exporter.getFinishedSpanItems().toString()).doesNotContain("synthetic-token");
    }
}
