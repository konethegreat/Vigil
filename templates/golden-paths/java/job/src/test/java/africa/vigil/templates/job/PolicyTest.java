// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Job policy tests
package africa.vigil.templates.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import africa.vigil.templates.job.policy.PolicyContext;
import africa.vigil.templates.job.policy.PolicyUnavailableException;
import africa.vigil.templates.job.policy.UnavailableWarden;
import org.junit.jupiter.api.Test;

class PolicyTest {

    @Test
    void rejectsABlankTenant() {
        assertThatThrownBy(() -> new PolicyContext(" ", "batch-summary", "token"))
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
        assertThatThrownBy(() -> new PolicyContext("tenant-a", "batch-summary", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signedToken");
    }

    @Test
    void neverPrintsTheToken() {
        String rendered = new PolicyContext("tenant-a", "batch-summary", "synthetic-token").toString();

        assertThat(rendered).contains("tenant-a", "batch-summary").doesNotContain("synthetic-token");
    }

    @Test
    void theStubFailsClosed() {
        assertThatThrownBy(() -> new UnavailableWarden().authorize("tenant-a", "batch-summary", "token"))
                .isInstanceOf(PolicyUnavailableException.class);
    }
}
