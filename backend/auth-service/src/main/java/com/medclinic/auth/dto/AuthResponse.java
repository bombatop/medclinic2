package com.medclinic.auth.dto;

public record AuthResponse(
        String accessToken,
        String role,
        String username
) {}
