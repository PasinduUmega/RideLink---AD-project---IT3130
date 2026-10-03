package com.ridelink.ridemanagement.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service-to-service calls reuse the caller's JWT: every RideLink service validates the same
 * shared-secret token, so forwarding the incoming Authorization header is enough.
 */
final class BearerTokenForwarder {

    private BearerTokenForwarder() {
    }

    static void apply(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null) headers.set(HttpHeaders.AUTHORIZATION, auth);
        }
    }
}
