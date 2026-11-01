// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy type tests
package africa.vigil.templates.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import africa.vigil.templates.service.policy.PolicyContext;
import africa.vigil.templates.service.policy.PolicyDeniedException;
import africa.vigil.templates.service.policy.PolicyUnavailableException;
import africa.vigil.templates.service.policy.UnavailableWarden;
import org.junit.jupiter.api.Test;

class PolicyTypesTest {

    @Test
    void rejectsABlankTenant() {
        assertThatThrownBy(() -> new PolicyContext(" ", "investigation", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void rejectsABlankPurpose() {
        assertThatThrownBy(() -> new PolicyContext("tenant-a", "", "token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purpose");
    }

    @Test
    void rejectsABlankToken() {
        assertThatThrownBy(() -> new PolicyContext("tenant-a", "investigation", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signedToken");
    }

    @Test
    void neverPrintsTheToken() {
        String rendered = new PolicyContext("tenant-a", "investigation", "synthetic-token").toString();

        assertThat(rendered).contains("tenant-a", "investigation").doesNotContain("synthetic-token");
    }

    @Test
    void theStubIsNeverReady() {
        assertThat(new UnavailableWarden().ready()).isFalse();
    }

    @Test
    void failuresAreRuntimeExceptions() {
        assertThat(new PolicyUnavailableException("unavailable")).isInstanceOf(RuntimeException.class);
        assertThat(new PolicyDeniedException("denied")).isInstanceOf(RuntimeException.class);
    }
}
