// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Verified policy context for libraries (P1 task 1.4)
use std::error::Error;
use std::fmt;

/// A context the caller resolved through Warden.
///
/// A library never builds this type from the environment or a request. The caller constructs it
/// after verification and passes it explicitly; construction rejects blank tenant and purpose.
#[derive(Clone, PartialEq, Eq)]
pub struct PolicyContext {
    tenant_id: String,
    purpose: String,
    signed_token: Option<String>,
}

impl PolicyContext {
    /// Builds a context from a verified tenant and purpose.
    ///
    /// # Errors
    ///
    /// Returns [`PolicyContextError`] when either value is blank.
    pub fn new(
        tenant_id: impl Into<String>,
        purpose: impl Into<String>,
    ) -> Result<Self, PolicyContextError> {
        let tenant_id = tenant_id.into();
        if tenant_id.trim().is_empty() {
            return Err(PolicyContextError::BlankTenant);
        }
        let purpose = purpose.into();
        if purpose.trim().is_empty() {
            return Err(PolicyContextError::BlankPurpose);
        }
        Ok(Self {
            tenant_id,
            purpose,
            signed_token: None,
        })
    }

    /// Attaches the signed token Warden returned, if the caller has one.
    #[must_use]
    pub fn with_signed_token(mut self, signed_token: impl Into<String>) -> Self {
        self.signed_token = Some(signed_token.into());
        self
    }

    /// Returns the verified tenant.
    #[must_use]
    pub fn tenant_id(&self) -> &str {
        &self.tenant_id
    }

    /// Returns the verified purpose.
    #[must_use]
    pub fn purpose(&self) -> &str {
        &self.purpose
    }

    /// Returns the signed token, when the caller attached one.
    #[must_use]
    pub fn signed_token(&self) -> Option<&str> {
        self.signed_token.as_deref()
    }
}

impl fmt::Debug for PolicyContext {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("PolicyContext")
            .field("tenant_id", &self.tenant_id)
            .field("purpose", &self.purpose)
            .field(
                "signed_token",
                &self.signed_token.as_ref().map(|_| "redacted"),
            )
            .finish()
    }
}

/// Why a policy context was refused.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PolicyContextError {
    /// The tenant was empty or whitespace.
    BlankTenant,
    /// The purpose was empty or whitespace.
    BlankPurpose,
}

impl fmt::Display for PolicyContextError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::BlankTenant => formatter.write_str("tenant must not be blank"),
            Self::BlankPurpose => formatter.write_str("purpose must not be blank"),
        }
    }
}

impl Error for PolicyContextError {}
