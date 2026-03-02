package com.medclinic.main.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@RequiredArgsConstructor
public class RequestContext {

    private final HttpServletRequest request;

    public Long getUserId() {
        String header = request.getHeader("X-User-Id");
        if (header == null) {
            throw new IllegalStateException("Missing X-User-Id header");
        }
        return Long.parseLong(header);
    }

    public String getUsername() {
        String header = request.getHeader("X-Username");
        if (header == null) {
            throw new IllegalStateException("Missing X-Username header");
        }
        return header;
    }

    public String getRole() {
        String header = request.getHeader("X-User-Role");
        if (header == null) {
            throw new IllegalStateException("Missing X-User-Role header");
        }
        return header;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }
}
