package com.medclinic.main.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;
import java.util.Set;

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

    public List<String> getRoles() {
        return parseCsvHeader("X-User-Roles");
    }

    public Set<String> getPermissions() {
        return Set.copyOf(parseCsvHeader("X-User-Permissions"));
    }

    public boolean isAdmin() {
        return getRoles().contains("ADMIN");
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }

    public boolean hasAnyPermission(String... permissions) {
        Set<String> granted = getPermissions();
        for (String permission : permissions) {
            if (granted.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseCsvHeader(String headerName) {
        String header = request.getHeader(headerName);
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
