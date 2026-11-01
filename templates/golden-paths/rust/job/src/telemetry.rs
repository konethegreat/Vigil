// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Opt-in OTLP telemetry (P1 task 1.4)
use std::error::Error;
use std::fmt;

use opentelemetry::KeyValue;
use opentelemetry::global;
use opentelemetry_otlp::{SpanExporter, WithExportConfig};
use opentelemetry_sdk::Resource;
use opentelemetry_sdk::trace::{BatchSpanProcessor, SdkTracerProvider};

use crate::config::Settings;

/// Why telemetry could not be configured.
#[derive(Debug)]
pub enum TelemetryError {
    /// The OTLP exporter could not be built.
    Exporter(opentelemetry_otlp::ExporterBuildError),
}

impl fmt::Display for TelemetryError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Exporter(error) => write!(formatter, "OTLP exporter: {error}"),
        }
    }
}

impl Error for TelemetryError {}

/// Builds the OTLP exporter, or returns `None` when no collector is configured.
///
/// # Errors
///
/// Returns [`TelemetryError`] when a configured endpoint cannot build an exporter.
pub fn otlp_exporter(settings: &Settings) -> Result<Option<SpanExporter>, TelemetryError> {
    let Some(endpoint) = settings.otel_endpoint.as_deref() else {
        return Ok(None);
    };
    let exporter = SpanExporter::builder()
        .with_http()
        .with_endpoint(endpoint)
        .build()
        .map_err(TelemetryError::Exporter)?;
    Ok(Some(exporter))
}

/// Installs a provider only when an in-boundary collector is configured.
///
/// # Errors
///
/// Returns [`TelemetryError`] when a configured endpoint cannot build an exporter.
pub fn start_telemetry(settings: &Settings) -> Result<Option<SdkTracerProvider>, TelemetryError> {
    let Some(exporter) = otlp_exporter(settings)? else {
        return Ok(None);
    };
    let resource = Resource::builder()
        .with_service_name(settings.job_name.clone())
        .with_attributes([KeyValue::new("service.version", env!("CARGO_PKG_VERSION"))])
        .build();
    let provider = SdkTracerProvider::builder()
        .with_resource(resource)
        .with_span_processor(BatchSpanProcessor::builder(exporter).build())
        .build();
    global::set_tracer_provider(provider.clone());
    Ok(Some(provider))
}
