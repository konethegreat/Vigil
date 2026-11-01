// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job policy seam tests (P1 task 1.4)
use vigil_job_template::policy::{
    AuthorizeRequest, PolicyContext, PolicyContextError, PolicyError, UnavailableWarden,
    WardenBinding,
};

fn request() -> AuthorizeRequest {
    AuthorizeRequest::new(
        "tenant-a",
        "claims-triage",
        "kms://vigil/dev/job#1".to_string(),
    )
}

#[test]
fn the_stub_is_never_ready() {
    assert!(!UnavailableWarden.ready());
}

#[test]
fn the_stub_refuses_every_request() {
    assert_eq!(
        UnavailableWarden.authorize(&request()),
        Err(PolicyError::Unavailable)
    );
}

#[test]
fn a_context_keeps_the_verified_tenant_and_purpose() {
    let context = PolicyContext::new("tenant-a", "claims-triage").expect("valid context");

    assert_eq!(context.tenant_id(), "tenant-a");
    assert_eq!(context.purpose(), "claims-triage");
}

#[test]
fn a_context_rejects_a_blank_tenant_or_purpose() {
    assert_eq!(
        PolicyContext::new(" ", "claims-triage"),
        Err(PolicyContextError::BlankTenant)
    );
    assert_eq!(
        PolicyContext::new("tenant-a", "\t"),
        Err(PolicyContextError::BlankPurpose)
    );
}

#[test]
fn the_errors_are_distinguishable_and_printable() {
    assert_eq!(PolicyError::Unavailable.to_string(), "unavailable");
    assert_eq!(PolicyError::Denied.to_string(), "denied");
    assert_eq!(
        PolicyContextError::BlankTenant.to_string(),
        "tenant must not be blank"
    );
    assert_eq!(
        PolicyContextError::BlankPurpose.to_string(),
        "purpose must not be blank"
    );
}
