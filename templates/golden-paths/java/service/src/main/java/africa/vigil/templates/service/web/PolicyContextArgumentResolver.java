// Written by Kone Tshivhinda (Qoder) on 2026-11-01 — Policy context argument resolver
package africa.vigil.templates.service.web;

import africa.vigil.templates.service.policy.PolicyContext;
import africa.vigil.templates.service.policy.WardenBinding;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves {@link PolicyContext} from the three verified request headers.
 *
 * <p>Missing headers are a 400. Warden failures propagate as their own exceptions so the advice
 * can map them. Error details never echo the header values (BACKEND_GUIDE B5).
 */
public final class PolicyContextArgumentResolver implements HandlerMethodArgumentResolver {

    static final String TENANT_HEADER = "X-Vigil-Tenant";
    static final String PURPOSE_HEADER = "X-Vigil-Purpose";
    static final String TOKEN_HEADER = "X-Vigil-Policy-Token";

    private final WardenBinding warden;

    public PolicyContextArgumentResolver(WardenBinding warden) {
        this.warden = warden;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == PolicyContext.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String tenant = request == null ? null : request.getHeader(TENANT_HEADER);
        String purpose = request == null ? null : request.getHeader(PURPOSE_HEADER);
        String token = request == null ? null : request.getHeader(TOKEN_HEADER);
        if (isBlank(tenant) || isBlank(purpose) || isBlank(token)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "verified policy context required");
        }
        return warden.authorize(tenant, purpose, token);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
