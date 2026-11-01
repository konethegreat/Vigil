// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Library golden path: caller-owned settings (P1 task 1.4)
//! Shared library golden path.
//!
//! A library receives configuration and a [`PolicyContext`] from its caller. It never reads the
//! environment, never builds a context and never opens a network connection.

mod policy;
mod settings;
mod summarise;

pub use policy::{PolicyContext, PolicyContextError};
pub use settings::{DEFAULT_BATCH_SIZE, LibrarySettings, LibrarySettingsError};
pub use summarise::{Summary, summarise, summarise_with_tracer};
