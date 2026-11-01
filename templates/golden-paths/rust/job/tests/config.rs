// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job configuration tests (P1 task 1.4)
use std::collections::HashMap;

use vigil_job_template::config::{DEFAULT_BATCH_SIZE, LogLevel, load_settings};

fn base() -> HashMap<String, String> {
    HashMap::from([
        ("VIGIL_TENANT_ID".to_string(), "tenant-a".to_string()),
        ("VIGIL_PURPOSE".to_string(), "claims-triage".to_string()),
        (
            "VIGIL_WARDEN_URL".to_string(),
            "http://warden.invalid:8080".to_string(),
        ),
        (
            "VIGIL_POLICY_TOKEN_REF".to_string(),
            "kms://vigil/dev/job#1".to_string(),
        ),
    ])
}

#[test]
fn applies_safe_defaults() {
    let settings = load_settings(&base()).expect("valid configuration");

    assert_eq!(settings.job_name, "vigil-job-template");
    assert_eq!(settings.tenant_id, "tenant-a");
    assert_eq!(settings.purpose, "claims-triage");
    assert_eq!(settings.warden_url, "http://warden.invalid:8080");
    assert_eq!(settings.policy_token_ref, "kms://vigil/dev/job#1");
    assert_eq!(settings.batch_size, DEFAULT_BATCH_SIZE);
    assert_eq!(settings.log_level, LogLevel::Info);
    assert_eq!(settings.otel_endpoint, None);
}

#[test]
fn reports_every_missing_required_value_at_once() {
    let error = load_settings(&HashMap::new()).expect_err("invalid configuration");
    let issues = error.issues();

    assert_eq!(issues.len(), 4);
    for key in [
        "VIGIL_TENANT_ID",
        "VIGIL_PURPOSE",
        "VIGIL_WARDEN_URL",
        "VIGIL_POLICY_TOKEN_REF",
    ] {
        assert!(
            issues.iter().any(|issue| issue.contains(key)),
            "no issue mentioned {key}: {issues:?}"
        );
    }
}

#[test]
fn rejects_a_blank_tenant_or_purpose() {
    let mut vars = base();
    vars.insert("VIGIL_TENANT_ID".to_string(), "   ".to_string());
    vars.insert("VIGIL_PURPOSE".to_string(), "\t".to_string());

    let error = load_settings(&vars).expect_err("invalid configuration");
    let issues = error.issues();

    assert!(issues.iter().any(|issue| issue.contains("VIGIL_TENANT_ID")));
    assert!(issues.iter().any(|issue| issue.contains("VIGIL_PURPOSE")));
}

#[test]
fn rejects_a_warden_url_that_is_not_http() {
    let mut vars = base();
    vars.insert(
        "VIGIL_WARDEN_URL".to_string(),
        "ftp://warden.invalid".to_string(),
    );

    let error = load_settings(&vars).expect_err("invalid configuration");

    assert!(
        error
            .issues()
            .iter()
            .any(|issue| issue.contains("VIGIL_WARDEN_URL"))
    );
}

#[test]
fn rejects_a_token_that_is_not_a_reference() {
    let mut vars = base();
    vars.insert(
        "VIGIL_POLICY_TOKEN_REF".to_string(),
        "not-a-secret-value".to_string(),
    );

    let error = load_settings(&vars).expect_err("invalid configuration");

    assert!(
        error
            .issues()
            .iter()
            .any(|i| i.contains("VIGIL_POLICY_TOKEN_REF"))
    );
    assert!(!error.to_string().contains("not-a-secret-value"));
}

#[test]
fn rejects_batch_sizes_outside_the_range() {
    for value in ["0", "abc", "-1", "10001"] {
        let mut vars = base();
        vars.insert("VIGIL_BATCH_SIZE".to_string(), value.to_string());

        let error = load_settings(&vars).expect_err("invalid batch size");

        assert!(
            error
                .issues()
                .iter()
                .any(|i| i.contains("VIGIL_BATCH_SIZE")),
            "accepted {value}"
        );
    }
}

#[test]
fn accepts_a_batch_size_at_the_limit() {
    let mut vars = base();
    vars.insert("VIGIL_BATCH_SIZE".to_string(), "10000".to_string());

    let settings = load_settings(&vars).expect("valid configuration");

    assert_eq!(settings.batch_size, 10_000);
}

#[test]
fn rejects_an_unknown_log_level() {
    let mut vars = base();
    vars.insert("VIGIL_LOG_LEVEL".to_string(), "DEBUG".to_string());

    let error = load_settings(&vars).expect_err("invalid log level");

    assert!(error.issues().iter().any(|i| i.contains("VIGIL_LOG_LEVEL")));
}

#[test]
fn accepts_every_documented_log_level() {
    for (raw, expected, info) in [
        ("INFO", LogLevel::Info, true),
        ("WARNING", LogLevel::Warning, false),
        ("ERROR", LogLevel::Error, false),
    ] {
        let mut vars = base();
        vars.insert("VIGIL_LOG_LEVEL".to_string(), raw.to_string());

        let settings = load_settings(&vars).expect("valid log level");

        assert_eq!(settings.log_level, expected);
        assert_eq!(settings.log_level.info_enabled(), info);
    }
}

#[test]
fn rejects_a_job_name_over_sixty_four_characters() {
    let mut vars = base();
    vars.insert("VIGIL_JOB_NAME".to_string(), "a".repeat(65));

    let error = load_settings(&vars).expect_err("invalid job name");

    assert!(error.issues().iter().any(|i| i.contains("VIGIL_JOB_NAME")));
}

#[test]
fn treats_a_blank_otel_endpoint_as_off() {
    let mut vars = base();
    vars.insert("VIGIL_OTEL_ENDPOINT".to_string(), String::new());

    let settings = load_settings(&vars).expect("valid configuration");

    assert_eq!(settings.otel_endpoint, None);
}

#[test]
fn reads_the_standard_otel_environment_name() {
    let mut vars = base();
    vars.insert(
        "OTEL_EXPORTER_OTLP_ENDPOINT".to_string(),
        "http://collector.invalid:4318".to_string(),
    );

    let settings = load_settings(&vars).expect("valid configuration");

    assert_eq!(
        settings.otel_endpoint.as_deref(),
        Some("http://collector.invalid:4318")
    );
}

#[test]
fn prefers_the_vigil_endpoint_over_the_standard_name() {
    let mut vars = base();
    vars.insert(
        "OTEL_EXPORTER_OTLP_ENDPOINT".to_string(),
        "http://standard.invalid:4318".to_string(),
    );
    vars.insert(
        "VIGIL_OTEL_ENDPOINT".to_string(),
        "http://vigil.invalid:4318".to_string(),
    );

    let settings = load_settings(&vars).expect("valid configuration");

    assert_eq!(
        settings.otel_endpoint.as_deref(),
        Some("http://vigil.invalid:4318")
    );
}

#[test]
fn rejects_an_otel_endpoint_that_is_not_http() {
    let mut vars = base();
    vars.insert(
        "VIGIL_OTEL_ENDPOINT".to_string(),
        "grpc://collector.invalid".to_string(),
    );

    let error = load_settings(&vars).expect_err("invalid endpoint");

    assert!(
        error
            .issues()
            .iter()
            .any(|i| i.contains("VIGIL_OTEL_ENDPOINT"))
    );
}

#[test]
fn renders_every_issue_in_one_message() {
    let error = load_settings(&HashMap::new()).expect_err("invalid configuration");

    let rendered = error.to_string();

    assert!(rendered.contains("VIGIL_TENANT_ID"));
    assert!(rendered.contains("VIGIL_WARDEN_URL"));
    assert!(rendered.contains("; "));
}
