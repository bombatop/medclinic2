package com.medclinic.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        java.util.List<String> roles,
        java.util.List<String> permissions,
        String username
) {}
