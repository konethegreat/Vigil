// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Summarise operation tests (P1 task 1.4)
use opentelemetry::trace::TracerProvider as _;
use opentelemetry_sdk::trace::{InMemorySpanExporter, SdkTracerProvider};

use vigil_library_template::{LibrarySettings, PolicyContext, summarise_with_tracer};

fn test_context() -> PolicyContext {
    PolicyContext::new("tenant-a", "claims-triage").expect("valid context")
}

fn test_settings() -> LibrarySettings {
    LibrarySettings::new("claims", 2).expect("valid settings")
}

fn capture() -> (SdkTracerProvider, InMemorySpanExporter) {
    let exporter = InMemorySpanExporter::default();
    let provider = SdkTracerProvider::builder()
        .with_simple_exporter(exporter.clone())
        .build();
    (provider, exporter)
}

#[test]
fn counts_records_and_reports_truncation() {
    let (provider, _exporter) = capture();
    let tracer = provider.tracer("test");
    let records = ["first", "  ", "second", "third"];

    let summary = summarise_with_tracer(&tracer, &records, &test_settings(), &test_context());

    assert_eq!(summary.namespace(), "claims");
    assert_eq!(summary.count(), 3);
    assert!(summary.truncated());
}

#[test]
fn does_not_truncate_within_the_batch_size() {
    let (provider, _exporter) = capture();
    let tracer = provider.tracer("test");
    let records = ["first", "second"];

    let summary = summarise_with_tracer(&tracer, &records, &test_settings(), &test_context());

    assert_eq!(summary.count(), 2);
    assert!(!summary.truncated());
}

#[test]
fn records_only_tenant_and_purpose_on_the_span() {
    let (provider, exporter) = capture();
    let tracer = provider.tracer("test");

    let summary = summarise_with_tracer(
        &tracer,
        &["secret record text"],
        &test_settings(),
        &test_context(),
    );
    assert_eq!(summary.count(), 1);

    let spans = exporter.get_finished_spans().expect("exported spans");
    assert_eq!(spans.len(), 1);
    let span = &spans[0];
    assert_eq!(span.name, "vigil.library.summarise");

    let attributes: Vec<(String, String)> = span
        .attributes
        .iter()
        .map(|kv| (kv.key.to_string(), kv.value.to_string()))
        .collect();
    assert_eq!(
        attributes,
        vec![
            ("vigil.tenant".to_string(), "tenant-a".to_string()),
            ("vigil.purpose".to_string(), "claims-triage".to_string()),
        ]
    );
}

#[test]
fn drops_blank_records_without_recording_them() {
    let (provider, exporter) = capture();
    let tracer = provider.tracer("test");

    let summary = summarise_with_tracer(
        &tracer,
        &["   ", "\t", ""],
        &test_settings(),
        &test_context(),
    );
    assert_eq!(summary.count(), 0);

    let spans = exporter.get_finished_spans().expect("exported spans");
    assert_eq!(spans[0].attributes.len(), 2);
    assert!(spans[0].events.is_empty());
}
