// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Example library operation (P1 task 1.4)
use opentelemetry::KeyValue;
use opentelemetry::global;
use opentelemetry::trace::{Span, Tracer};

use crate::policy::PolicyContext;
use crate::settings::LibrarySettings;

/// Result of one summarise operation.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Summary {
    namespace: String,
    count: usize,
    truncated: bool,
}

impl Summary {
    /// Returns the caller's namespace.
    #[must_use]
    pub fn namespace(&self) -> &str {
        &self.namespace
    }

    /// Returns how many non-blank records were counted.
    #[must_use]
    pub fn count(&self) -> usize {
        self.count
    }

    /// Returns whether the records exceeded the caller's batch size.
    #[must_use]
    pub fn truncated(&self) -> bool {
        self.truncated
    }
}

/// Counts the records with the process-wide tracer.
///
/// The caller must have installed a provider; the library never sets one up.
#[must_use]
pub fn summarise(records: &[&str], settings: &LibrarySettings, context: &PolicyContext) -> Summary {
    let tracer = global::tracer("vigil.library");
    summarise_with_tracer(&tracer, records, settings, context)
}

/// Counts the records with the tracer the caller supplies.
///
/// The span records the tenant and purpose only. Record content never reaches telemetry.
#[must_use]
pub fn summarise_with_tracer<T: Tracer>(
    tracer: &T,
    records: &[&str],
    settings: &LibrarySettings,
    context: &PolicyContext,
) -> Summary {
    let mut span = tracer.start("vigil.library.summarise");
    span.set_attribute(KeyValue::new(
        "vigil.tenant",
        context.tenant_id().to_owned(),
    ));
    span.set_attribute(KeyValue::new("vigil.purpose", context.purpose().to_owned()));
    let count = records
        .iter()
        .filter(|record| !record.trim().is_empty())
        .count();
    span.end();
    Summary {
        namespace: settings.namespace().to_owned(),
        count,
        truncated: count > settings.batch_size(),
    }
}
