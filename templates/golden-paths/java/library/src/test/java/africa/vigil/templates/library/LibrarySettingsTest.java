// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Library settings tests
package africa.vigil.templates.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LibrarySettingsTest {

    @Test
    void rejectsBlankNamespace() {
        assertThatThrownBy(() -> new LibrarySettings(" ", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("namespace");
    }

    @Test
    void rejectsNonPositiveBatchSize() {
        assertThatThrownBy(() -> new LibrarySettings("summary", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize");
    }

    @Test
    void keepsInjectedValues() {
        LibrarySettings settings = new LibrarySettings("summary", 25);

        assertThat(settings.namespace()).isEqualTo("summary");
        assertThat(settings.batchSize()).isEqualTo(25);
    }
}
