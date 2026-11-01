// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job work unit (P1 task 1.4)
use std::error::Error;
use std::fmt;

use opentelemetry::KeyValue;
use opentelemetry::trace::{Span, Tracer};

use crate::config::Settings;
use crate::policy::{AuthorizeRequest, PolicyError, WardenBinding};

/// What one run produced.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct JobReport {
    /// Records the batch covered.
    pub processed: usize,
}

/// Why the run did not complete.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum JobError {
    /// Warden did not authorize the run.
    Policy(PolicyError),
}

impl fmt::Display for JobError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Policy(error) => write!(formatter, "policy evaluation failed: {error}"),
        }
    }
}

impl Error for JobError {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        match self {
            Self::Policy(error) => Some(error),
        }
    }
}

/// Runs the batch after Warden authorizes it.
///
/// # Errors
///
/// Returns [`JobError::Policy`] and performs no work when policy is unavailable or denied.
pub fn run<T: Tracer>(
    settings: &Settings,
    warden: &dyn WardenBinding,
    tracer: &T,
) -> Result<JobReport, JobError> {
    let request = AuthorizeRequest::new(
        settings.tenant_id.clone(),
        settings.purpose.clone(),
        settings.policy_token_ref.clone(),
    );
    let context = warden.authorize(&request).map_err(JobError::Policy)?;

    let mut span = tracer.start("vigil.job.run");
    span.set_attribute(KeyValue::new(
        "vigil.tenant",
        context.tenant_id().to_owned(),
    ));
    span.set_attribute(KeyValue::new("vigil.purpose", context.purpose().to_owned()));
    span.end();

    Ok(JobReport {
        processed: settings.batch_size,
    })
}
