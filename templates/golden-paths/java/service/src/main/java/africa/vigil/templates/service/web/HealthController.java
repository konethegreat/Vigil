// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Health probes
package africa.vigil.templates.service.web;

import africa.vigil.templates.service.policy.WardenBinding;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness, startup and readiness probes with the documented fail-closed semantics. */
@RestController
public final class HealthController {

    private final WardenBinding warden;

    public HealthController(WardenBinding warden) {
        this.warden = warden;
    }

    @GetMapping("/healthz/live")
    public ResponseEntity<Void> live() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/healthz/startup")
    public ResponseEntity<Void> startup() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/healthz/ready")
    public ResponseEntity<Void> ready() {
        return warden.ready()
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
