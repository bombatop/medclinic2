package com.medclinic.auth.dto;

import com.medclinic.auth.model.Role;

import java.time.Instant;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean active,
        boolean system,
        Instant createdAt
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                role.isSystem(),
                role.getCreatedAt()
        );
    }
}
