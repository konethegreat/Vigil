// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Caller-injected library settings (P1 task 1.4)
use std::error::Error;
use std::fmt;

/// Batch size used when the caller does not choose one.
pub const DEFAULT_BATCH_SIZE: usize = 100;

/// Settings a caller passes to a library operation.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct LibrarySettings {
    namespace: String,
    batch_size: usize,
}

impl LibrarySettings {
    /// Validates settings supplied by the caller.
    ///
    /// # Errors
    ///
    /// Returns [`LibrarySettingsError`] when the namespace is blank or the batch size is zero.
    pub fn new(
        namespace: impl Into<String>,
        batch_size: usize,
    ) -> Result<Self, LibrarySettingsError> {
        let namespace = namespace.into();
        if namespace.trim().is_empty() {
            return Err(LibrarySettingsError::BlankNamespace);
        }
        if batch_size == 0 {
            return Err(LibrarySettingsError::ZeroBatchSize);
        }
        Ok(Self {
            namespace,
            batch_size,
        })
    }

    /// Returns the namespace the caller selected.
    #[must_use]
    pub fn namespace(&self) -> &str {
        &self.namespace
    }

    /// Returns the largest batch the caller allows.
    #[must_use]
    pub fn batch_size(&self) -> usize {
        self.batch_size
    }
}

/// Why caller-supplied settings were refused.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum LibrarySettingsError {
    /// The namespace was empty or whitespace.
    BlankNamespace,
    /// The batch size was zero.
    ZeroBatchSize,
}

impl fmt::Display for LibrarySettingsError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::BlankNamespace => formatter.write_str("namespace must not be blank"),
            Self::ZeroBatchSize => formatter.write_str("batch size must be greater than zero"),
        }
    }
}

impl Error for LibrarySettingsError {}
