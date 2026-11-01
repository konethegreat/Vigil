// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Library summarise tests
package africa.vigil.templates.library;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SummariesTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider =
            SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
    private final Tracer tracer = provider.get("test");
    private final LibrarySettings settings = new LibrarySettings("summary", 2);
    private final PolicyContext context = new PolicyContext("tenant-a", "investigation");

    @BeforeEach
    void resetSpans() {
        exporter.reset();
    }

    @AfterEach
    void shutdownProvider() {
        provider.close();
    }

    @Test
    void reportsNamespaceAndTotalCount() {
        Summary summary = Summaries.summarise(settings, context, List.of("a", "b"), tracer);

        assertThat(summary.namespace()).isEqualTo("summary");
        assertThat(summary.count()).isEqualTo(2);
    }

    @Test
    void doesNotTruncateAtTheBatchSize() {
        Summary summary = Summaries.summarise(settings, context, List.of("a", "b"), tracer);

        assertThat(summary.truncated()).isFalse();
    }

    @Test
    void flagsTruncationAboveTheBatchSize() {
        Summary summary = Summaries.summarise(settings, context, List.of("a", "b", "c"), tracer);

        assertThat(summary.count()).isEqualTo(3);
        assertThat(summary.truncated()).isTrue();
    }

    @Test
    void attachesOnlyTenantAndPurposeToTheSpan() {
        Summaries.summarise(settings, context, List.of("a"), tracer);

        AttributeKey<String> tenant = AttributeKey.stringKey("vigil.tenant");
        AttributeKey<String> purpose = AttributeKey.stringKey("vigil.purpose");
        assertThat(exporter.getFinishedSpanItems()).singleElement().satisfies(span -> {
            assertThat(span.getName()).isEqualTo("vigil.library.summarise");
            assertThat(span.getAttributes().asMap()).containsOnlyKeys(tenant, purpose);
            assertThat(span.getAttributes().get(tenant)).isEqualTo("tenant-a");
            assertThat(span.getAttributes().get(purpose)).isEqualTo("investigation");
        });
    }

    @Test
    void worksWithoutAnExporterBecauseTheCallerOwnsTelemetry() {
        Tracer noop = TracerProvider.noop().get("test");

        Summary summary = Summaries.summarise(settings, context, List.of("a"), noop);

        assertThat(summary.count()).isEqualTo(1);
        assertThat(exporter.getFinishedSpanItems()).isEmpty();
    }
}
