// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Library settings tests (P1 task 1.4)
use vigil_library_template::{DEFAULT_BATCH_SIZE, LibrarySettings, LibrarySettingsError};

#[test]
fn keeps_a_valid_namespace_and_batch_size() {
    let settings = LibrarySettings::new("claims", 25).expect("valid settings");

    assert_eq!(settings.namespace(), "claims");
    assert_eq!(settings.batch_size(), 25);
}

#[test]
fn defaults_to_one_hundred_records() {
    assert_eq!(DEFAULT_BATCH_SIZE, 100);
}

#[test]
fn rejects_a_blank_namespace() {
    assert_eq!(
        LibrarySettings::new(" \t ", DEFAULT_BATCH_SIZE),
        Err(LibrarySettingsError::BlankNamespace)
    );
}

#[test]
fn rejects_a_zero_batch_size() {
    assert_eq!(
        LibrarySettings::new("claims", 0),
        Err(LibrarySettingsError::ZeroBatchSize)
    );
}

#[test]
fn reports_the_reason_for_invalid_settings() {
    assert_eq!(
        LibrarySettingsError::BlankNamespace.to_string(),
        "namespace must not be blank"
    );
    assert_eq!(
        LibrarySettingsError::ZeroBatchSize.to_string(),
        "batch size must be greater than zero"
    );
}
