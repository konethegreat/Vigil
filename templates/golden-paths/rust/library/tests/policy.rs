// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy context tests (P1 task 1.4)
use vigil_library_template::{PolicyContext, PolicyContextError};

#[test]
fn accepts_a_verified_context() {
    let context = PolicyContext::new("tenant-a", "claims-triage").expect("valid context");

    assert_eq!(context.tenant_id(), "tenant-a");
    assert_eq!(context.purpose(), "claims-triage");
    assert_eq!(context.signed_token(), None);
}

#[test]
fn carries_an_optional_signed_token() {
    let context = PolicyContext::new("tenant-a", "claims-triage")
        .expect("valid context")
        .with_signed_token("signed-token");

    assert_eq!(context.signed_token(), Some("signed-token"));
}

#[test]
fn rejects_a_blank_tenant() {
    assert_eq!(
        PolicyContext::new("  ", "claims-triage"),
        Err(PolicyContextError::BlankTenant)
    );
}

#[test]
fn rejects_a_blank_purpose() {
    assert_eq!(
        PolicyContext::new("tenant-a", "\t"),
        Err(PolicyContextError::BlankPurpose)
    );
}

#[test]
fn never_prints_the_signed_token() {
    let context = PolicyContext::new("tenant-a", "claims-triage")
        .expect("valid context")
        .with_signed_token("do-not-log-this");

    let rendered = format!("{context:?}");

    assert!(rendered.contains("tenant-a"));
    assert!(rendered.contains("claims-triage"));
    assert!(!rendered.contains("do-not-log-this"));
    assert!(rendered.contains("redacted"));
}

#[test]
fn reports_the_reason_for_missing_context() {
    assert_eq!(
        PolicyContextError::BlankTenant.to_string(),
        "tenant must not be blank"
    );
    assert_eq!(
        PolicyContextError::BlankPurpose.to_string(),
        "purpose must not be blank"
    );
}
