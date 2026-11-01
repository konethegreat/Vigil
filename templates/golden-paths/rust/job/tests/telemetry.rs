// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Opt-in telemetry tests (P1 task 1.4)
use vigil_job_template::config::{LogLevel, Settings};
use vigil_job_template::telemetry::{otlp_exporter, start_telemetry};

fn settings(otel_endpoint: Option<&str>) -> Settings {
    Settings {
        job_name: "vigil-job-template".to_string(),
        tenant_id: "tenant-a".to_string(),
        purpose: "claims-triage".to_string(),
        warden_url: "http://warden.invalid:8080".to_string(),
        policy_token_ref: "kms://vigil/dev/job#1".to_string(),
        batch_size: 100,
        log_level: LogLevel::Info,
        otel_endpoint: otel_endpoint.map(str::to_string),
    }
}

#[test]
fn builds_no_exporter_without_an_endpoint() {
    assert!(
        otlp_exporter(&settings(None))
            .expect("no exporter error")
            .is_none()
    );
    assert!(
        start_telemetry(&settings(None))
            .expect("no provider error")
            .is_none()
    );
}

#[test]
fn builds_an_exporter_only_for_a_configured_endpoint() {
    let exporter =
        otlp_exporter(&settings(Some("http://collector.invalid:4318"))).expect("exporter builds");

    assert!(exporter.is_some());
}

#[test]
fn installs_a_provider_for_a_configured_endpoint() {
    let provider = start_telemetry(&settings(Some("http://collector.invalid:4318")))
        .expect("provider")
        .expect("configured endpoint");

    provider.shutdown().expect("provider shutdown");
}
