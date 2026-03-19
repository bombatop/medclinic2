package com.medclinic.auth.dto;

import java.util.List;

public record UserRolesResponse(
        Long userId,
        String username,
        List<String> roles
) {}
