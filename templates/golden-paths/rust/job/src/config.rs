// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job configuration from the environment (P1 task 1.4)
use std::collections::HashMap;
use std::error::Error;
use std::fmt;

/// Default number of records one run processes.
pub const DEFAULT_BATCH_SIZE: usize = 100;
/// Largest batch a job may request.
pub const MAX_BATCH_SIZE: usize = 10_000;
/// Largest accepted job name.
pub const MAX_JOB_NAME: usize = 64;

/// Verbosity the job uses for its own stderr messages.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum LogLevel {
    /// Normal progress output.
    Info,
    /// Warnings only.
    Warning,
    /// Errors only.
    Error,
}

impl LogLevel {
    fn parse(value: &str) -> Option<Self> {
        match value {
            "INFO" => Some(Self::Info),
            "WARNING" => Some(Self::Warning),
            "ERROR" => Some(Self::Error),
            _ => None,
        }
    }

    /// Returns whether progress output is enabled.
    #[must_use]
    pub fn info_enabled(self) -> bool {
        self == Self::Info
    }
}

/// Validated job configuration.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Settings {
    /// Name of this job, used as the service name in telemetry.
    pub job_name: String,
    /// Verified tenant the batch operates for.
    pub tenant_id: String,
    /// Verified purpose that bounds the batch.
    pub purpose: String,
    /// In-boundary Warden endpoint.
    pub warden_url: String,
    /// `kms://` reference to the policy token; never the token value.
    pub policy_token_ref: String,
    /// Records processed in one run.
    pub batch_size: usize,
    /// Verbosity for the job's own messages.
    pub log_level: LogLevel,
    /// In-boundary OTLP endpoint; `None` means no export and no egress.
    pub otel_endpoint: Option<String>,
}

/// Every problem found while reading the environment.
#[derive(Debug, PartialEq, Eq)]
pub struct ConfigurationError {
    issues: Vec<String>,
}

impl ConfigurationError {
    /// Returns one message per invalid or missing value.
    #[must_use]
    pub fn issues(&self) -> &[String] {
        &self.issues
    }
}

impl fmt::Display for ConfigurationError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(formatter, "{}", self.issues.join("; "))
    }
}

impl Error for ConfigurationError {}

fn optional(vars: &HashMap<String, String>, key: &str) -> Option<String> {
    vars.get(key)
        .map(|value| value.trim())
        .filter(|value| !value.is_empty())
        .map(str::to_owned)
}

fn required(vars: &HashMap<String, String>, key: &str, issues: &mut Vec<String>) -> Option<String> {
    let value = optional(vars, key);
    if value.is_none() {
        issues.push(format!("{key} is required"));
    }
    value
}

fn is_http_url(value: &str) -> bool {
    let Some(rest) = value
        .strip_prefix("http://")
        .or_else(|| value.strip_prefix("https://"))
    else {
        return false;
    };
    !rest.is_empty() && !rest.chars().any(char::is_whitespace)
}

fn is_kms_reference(value: &str) -> bool {
    value
        .strip_prefix("kms://")
        .is_some_and(|path| !path.trim().is_empty())
}

/// Reads and validates the job environment.
///
/// # Errors
///
/// Returns [`ConfigurationError`] listing every invalid or missing value, so one run reports all
/// problems instead of one at a time.
pub fn load_settings(vars: &HashMap<String, String>) -> Result<Settings, ConfigurationError> {
    let mut issues = Vec::new();

    let job_name =
        optional(vars, "VIGIL_JOB_NAME").unwrap_or_else(|| "vigil-job-template".to_string());
    if job_name.chars().count() > MAX_JOB_NAME {
        issues.push(format!(
            "VIGIL_JOB_NAME must be 1 to {MAX_JOB_NAME} characters"
        ));
    }

    let tenant_id = required(vars, "VIGIL_TENANT_ID", &mut issues).unwrap_or_default();
    let purpose = required(vars, "VIGIL_PURPOSE", &mut issues).unwrap_or_default();

    let warden_url = required(vars, "VIGIL_WARDEN_URL", &mut issues).unwrap_or_default();
    if !warden_url.is_empty() && !is_http_url(&warden_url) {
        issues.push("VIGIL_WARDEN_URL must be an http:// or https:// URL".to_string());
    }

    let policy_token_ref =
        required(vars, "VIGIL_POLICY_TOKEN_REF", &mut issues).unwrap_or_default();
    if !policy_token_ref.is_empty() && !is_kms_reference(&policy_token_ref) {
        issues.push(
            "VIGIL_POLICY_TOKEN_REF must be a kms:// reference, never a token value".to_string(),
        );
    }

    let batch_size = match optional(vars, "VIGIL_BATCH_SIZE") {
        None => DEFAULT_BATCH_SIZE,
        Some(raw) => match raw.parse::<usize>() {
            Ok(value) if (1..=MAX_BATCH_SIZE).contains(&value) => value,
            _ => {
                issues.push(format!(
                    "VIGIL_BATCH_SIZE must be an integer from 1 to {MAX_BATCH_SIZE}"
                ));
                DEFAULT_BATCH_SIZE
            }
        },
    };

    let log_level = match optional(vars, "VIGIL_LOG_LEVEL") {
        None => LogLevel::Info,
        Some(raw) => LogLevel::parse(&raw).unwrap_or_else(|| {
            issues.push("VIGIL_LOG_LEVEL must be INFO, WARNING or ERROR".to_string());
            LogLevel::Info
        }),
    };

    let otel_endpoint = optional(vars, "VIGIL_OTEL_ENDPOINT")
        .or_else(|| optional(vars, "OTEL_EXPORTER_OTLP_ENDPOINT"));
    let otel_endpoint = match otel_endpoint {
        Some(endpoint) if is_http_url(&endpoint) => Some(endpoint),
        Some(_) => {
            issues.push("VIGIL_OTEL_ENDPOINT must be an http:// or https:// URL".to_string());
            None
        }
        None => None,
    };

    if issues.is_empty() {
        Ok(Settings {
            job_name,
            tenant_id,
            purpose,
            warden_url,
            policy_token_ref,
            batch_size,
            log_level,
            otel_endpoint,
        })
    } else {
        Err(ConfigurationError { issues })
    }
}
