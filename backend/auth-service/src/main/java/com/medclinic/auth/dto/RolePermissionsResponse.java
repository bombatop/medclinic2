package com.medclinic.auth.dto;

import java.util.List;

public record RolePermissionsResponse(
        Long roleId,
        String roleCode,
        List<String> permissions
) {}
