// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job run path tests (P1 task 1.4)
use std::cell::RefCell;

use opentelemetry::trace::TracerProvider as _;
use opentelemetry_sdk::trace::{InMemorySpanExporter, SdkTracerProvider};

use vigil_job_template::config::{LogLevel, Settings};
use vigil_job_template::policy::{AuthorizeRequest, PolicyContext, PolicyError, WardenBinding};
use vigil_job_template::run::{JobError, run};

fn settings() -> Settings {
    Settings {
        job_name: "vigil-job-template".to_string(),
        tenant_id: "tenant-a".to_string(),
        purpose: "claims-triage".to_string(),
        warden_url: "http://warden.invalid:8080".to_string(),
        policy_token_ref: "kms://vigil/dev/job#1".to_string(),
        batch_size: 25,
        log_level: LogLevel::Info,
        otel_endpoint: None,
    }
}

struct FixedWarden {
    result: Result<PolicyContext, PolicyError>,
    seen: RefCell<Option<(String, String, String)>>,
}

impl FixedWarden {
    fn allowing() -> Self {
        Self {
            result: Ok(PolicyContext::new("tenant-a", "claims-triage").expect("valid context")),
            seen: RefCell::new(None),
        }
    }

    fn refusing(error: PolicyError) -> Self {
        Self {
            result: Err(error),
            seen: RefCell::new(None),
        }
    }
}

impl WardenBinding for FixedWarden {
    fn ready(&self) -> bool {
        true
    }

    fn authorize(&self, request: &AuthorizeRequest) -> Result<PolicyContext, PolicyError> {
        *self.seen.borrow_mut() = Some((
            request.tenant_id.clone(),
            request.purpose.clone(),
            request.token_ref.clone(),
        ));
        self.result.clone()
    }
}

fn capture() -> (SdkTracerProvider, InMemorySpanExporter) {
    let exporter = InMemorySpanExporter::default();
    let provider = SdkTracerProvider::builder()
        .with_simple_exporter(exporter.clone())
        .build();
    (provider, exporter)
}

#[test]
fn runs_only_after_authorization_and_reports_the_batch() {
    let (provider, exporter) = capture();
    let tracer = provider.tracer("test");
    let warden = FixedWarden::allowing();

    let report = run(&settings(), &warden, &tracer).expect("authorized run");

    assert_eq!(report.processed, 25);
    let seen = warden.seen.borrow().clone().expect("warden called");
    assert_eq!(
        seen,
        (
            "tenant-a".to_string(),
            "claims-triage".to_string(),
            "kms://vigil/dev/job#1".to_string()
        )
    );
    let spans = exporter.get_finished_spans().expect("exported spans");
    assert_eq!(spans.len(), 1);
    assert_eq!(spans[0].name, "vigil.job.run");
    let attributes: Vec<(String, String)> = spans[0]
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
fn refuses_to_run_when_policy_is_unavailable() {
    let (provider, exporter) = capture();
    let tracer = provider.tracer("test");

    let result = run(
        &settings(),
        &FixedWarden::refusing(PolicyError::Unavailable),
        &tracer,
    );

    assert_eq!(result, Err(JobError::Policy(PolicyError::Unavailable)));
    assert!(exporter.get_finished_spans().expect("spans").is_empty());
}

#[test]
fn refuses_to_run_when_policy_denies() {
    let (provider, exporter) = capture();
    let tracer = provider.tracer("test");

    let result = run(
        &settings(),
        &FixedWarden::refusing(PolicyError::Denied),
        &tracer,
    );

    assert_eq!(result, Err(JobError::Policy(PolicyError::Denied)));
    assert!(exporter.get_finished_spans().expect("spans").is_empty());
}

#[test]
fn explains_policy_failures_and_keeps_the_cause() {
    use std::error::Error;

    let error = JobError::Policy(PolicyError::Denied);

    assert_eq!(error.to_string(), "policy evaluation failed: denied");
    let source = error.source().expect("policy error is the cause");
    assert_eq!(source.to_string(), "denied");
}
