// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job policy seam (P1 task 1.4)
use std::error::Error;
use std::fmt;

/// What the job asks Warden to authorize.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct AuthorizeRequest {
    /// Tenant the batch operates for.
    pub tenant_id: String,
    /// Purpose that bounds the batch.
    pub purpose: String,
    /// `kms://` reference to the policy token.
    pub token_ref: String,
}

impl AuthorizeRequest {
    /// Builds an authorization request.
    #[must_use]
    pub fn new(
        tenant_id: impl Into<String>,
        purpose: impl Into<String>,
        token_ref: impl Into<String>,
    ) -> Self {
        Self {
            tenant_id: tenant_id.into(),
            purpose: purpose.into(),
            token_ref: token_ref.into(),
        }
    }
}

/// The context Warden returned for this run.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct PolicyContext {
    tenant_id: String,
    purpose: String,
}

impl PolicyContext {
    /// Builds a context from verified values.
    ///
    /// # Errors
    ///
    /// Returns [`PolicyContextError`] when a value is blank, so a malformed decision fails closed.
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
        Ok(Self { tenant_id, purpose })
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
}

/// Why a decision could not be used.
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

/// Why Warden did not produce a context.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PolicyError {
    /// Warden could not be reached or could not decide.
    Unavailable,
    /// Warden denied the request.
    Denied,
}

impl fmt::Display for PolicyError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Unavailable => formatter.write_str("unavailable"),
            Self::Denied => formatter.write_str("denied"),
        }
    }
}

impl Error for PolicyError {}

/// The seam the P3 Warden client implements.
pub trait WardenBinding {
    /// Returns whether the binding can evaluate decisions.
    fn ready(&self) -> bool;

    /// Evaluates one request.
    ///
    /// # Errors
    ///
    /// Returns [`PolicyError`] rather than a permissive context.
    fn authorize(&self, request: &AuthorizeRequest) -> Result<PolicyContext, PolicyError>;
}

/// The stub in place until P3. It never allows a run.
#[derive(Clone, Copy, Debug, Default)]
pub struct UnavailableWarden;

impl WardenBinding for UnavailableWarden {
    fn ready(&self) -> bool {
        false
    }

    fn authorize(&self, _request: &AuthorizeRequest) -> Result<PolicyContext, PolicyError> {
        Err(PolicyError::Unavailable)
    }
}
