// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Exit-code tests for the job binary (P1 task 1.4)
use std::process::Command;

fn job() -> Command {
    let mut command = Command::new(env!("CARGO_BIN_EXE_vigil-job-template"));
    let inherited: Vec<String> = std::env::vars()
        .map(|(key, _)| key)
        .filter(|key| key.starts_with("VIGIL_") || key.starts_with("OTEL_"))
        .collect();
    for key in inherited {
        command.env_remove(key);
    }
    command
}

#[test]
fn exits_two_without_configuration() {
    let output = job().output().expect("run the job");

    assert_eq!(output.status.code(), Some(2));
    let stderr = String::from_utf8_lossy(&output.stderr);
    assert!(stderr.contains("configuration is invalid"), "{stderr}");
}

#[test]
fn exits_three_while_the_warden_binding_is_unavailable() {
    let output = job()
        .env("VIGIL_TENANT_ID", "tenant-a")
        .env("VIGIL_PURPOSE", "claims-triage")
        .env("VIGIL_WARDEN_URL", "http://warden.invalid:8080")
        .env("VIGIL_POLICY_TOKEN_REF", "kms://vigil/dev/job#1")
        .output()
        .expect("run the job");

    assert_eq!(output.status.code(), Some(3));
    let stderr = String::from_utf8_lossy(&output.stderr);
    assert!(stderr.contains("refusing to run"), "{stderr}");
    assert!(!stderr.contains("kms://vigil/dev/job#1"), "{stderr}");
}
