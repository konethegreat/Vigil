# Written by Kone Tshivhinda (Qoder) on 2026-10-31 — Library configuration tests
import pytest

from vigil_library_template.config import LibrarySettings


def test_defaults_are_applied() -> None:
    settings = LibrarySettings(namespace="synthetic")
    assert settings.batch_size == 100


def test_namespace_must_not_be_blank() -> None:
    with pytest.raises(ValueError, match="namespace"):
        LibrarySettings(namespace="   ")


def test_batch_size_must_be_positive() -> None:
    with pytest.raises(ValueError, match="batch_size"):
        LibrarySettings(namespace="synthetic", batch_size=0)
