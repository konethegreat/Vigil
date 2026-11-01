// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Default tracer path test (P1 task 1.4)
use opentelemetry::global;
use opentelemetry_sdk::trace::{InMemorySpanExporter, SdkTracerProvider};

use vigil_library_template::{LibrarySettings, PolicyContext, summarise};

#[test]
fn uses_the_global_tracer_by_default() {
    let exporter = InMemorySpanExporter::default();
    let provider = SdkTracerProvider::builder()
        .with_simple_exporter(exporter.clone())
        .build();
    global::set_tracer_provider(provider.clone());

    let settings = LibrarySettings::new("claims", 100).expect("valid settings");
    let context = PolicyContext::new("tenant-a", "claims-triage").expect("valid context");

    let summary = summarise(&["first", "second"], &settings, &context);

    assert_eq!(summary.count(), 2);
    let spans = exporter.get_finished_spans().expect("exported spans");
    assert_eq!(spans.len(), 1);
    assert_eq!(spans[0].name, "vigil.library.summarise");
    let _ = provider.shutdown();
}
