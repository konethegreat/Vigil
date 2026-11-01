// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Library policy tests
package africa.vigil.templates.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PolicyContextTest {

    @Test
    void rejectsBlankTenant() {
        assertThatThrownBy(() -> new PolicyContext(" ", "investigation"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void rejectsBlankPurpose() {
        assertThatThrownBy(() -> new PolicyContext("tenant-a", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purpose");
    }

    @Test
    void keepsVerifiedValues() {
        PolicyContext context = new PolicyContext("tenant-a", "investigation");

        assertThat(context.tenantId()).isEqualTo("tenant-a");
        assertThat(context.purpose()).isEqualTo("investigation");
    }
}
