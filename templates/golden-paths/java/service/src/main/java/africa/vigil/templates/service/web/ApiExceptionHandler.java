// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy failure mapping
package africa.vigil.templates.service.web;

import africa.vigil.templates.service.policy.PolicyDeniedException;
import africa.vigil.templates.service.policy.PolicyUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps policy failures to generic, non-echoing responses. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(PolicyUnavailableException.class)
    ProblemDetail unavailable() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "policy decision unavailable");
    }

    @ExceptionHandler(PolicyDeniedException.class)
    ProblemDetail denied() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "policy decision denied");
    }
}
