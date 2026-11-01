// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Guarded example route
package africa.vigil.templates.service.web;

import africa.vigil.templates.service.policy.PolicyContext;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The example route: replace it with the real contract, but keep the policy argument. */
@RestController
final class ExampleController {

    @GetMapping("/v1/example")
    Map<String, String> example(PolicyContext context) {
        return Map.of("tenant", context.tenantId(), "purpose", context.purpose());
    }
}
