// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job golden path: validated environment (P1 task 1.4)
//! Job golden path.
//!
//! A one-shot workload that resolves policy before it starts, exports telemetry only when an
//! in-boundary collector is configured, and reads secrets as `kms://` references.

pub mod config;
pub mod policy;
pub mod run;
pub mod telemetry;

/// The job finished its batch.
pub const EXIT_OK: u8 = 0;
/// The environment was invalid; nothing ran.
pub const EXIT_CONFIG: u8 = 2;
/// Policy was unavailable or denied the run.
pub const EXIT_POLICY: u8 = 3;
