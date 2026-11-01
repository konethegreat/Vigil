// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job entry point and exit codes (P1 task 1.4)
//!
//! Exit codes are part of the contract: 0 success, 2 invalid configuration, 3 policy unavailable.
//! The template returns 3 until the P3 binding replaces [`UnavailableWarden`].

use std::collections::HashMap;
use std::process::ExitCode;

use opentelemetry::global;

use vigil_job_template::config::load_settings;
use vigil_job_template::policy::UnavailableWarden;
use vigil_job_template::run::run;
use vigil_job_template::telemetry::start_telemetry;
use vigil_job_template::{EXIT_CONFIG, EXIT_OK, EXIT_POLICY};

fn main() -> ExitCode {
    let vars: HashMap<String, String> = std::env::vars().collect();
    let settings = match load_settings(&vars) {
        Ok(settings) => settings,
        Err(error) => {
            eprintln!("configuration is invalid: {error}");
            return ExitCode::from(EXIT_CONFIG);
        }
    };
    let provider = match start_telemetry(&settings) {
        Ok(provider) => provider,
        Err(error) => {
            eprintln!("telemetry configuration is invalid: {error}");
            return ExitCode::from(EXIT_CONFIG);
        }
    };

    let tracer = global::tracer("vigil.job");
    let code = match run(&settings, &UnavailableWarden, &tracer) {
        Ok(report) => {
            if settings.log_level.info_enabled() {
                eprintln!("job={} processed={}", settings.job_name, report.processed);
            }
            EXIT_OK
        }
        Err(error) => {
            eprintln!("{error}; refusing to run");
            EXIT_POLICY
        }
    };

    if let Some(provider) = provider {
        let _ = provider.shutdown();
    }
    ExitCode::from(code)
}
