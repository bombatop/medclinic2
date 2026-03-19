package com.medclinic.auth.dto;

import com.medclinic.auth.model.PermissionEntity;

import java.time.Instant;

public record PermissionResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean active,
        boolean system,
        Instant createdAt
) {
    public static PermissionResponse from(PermissionEntity permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.isActive(),
                permission.isSystem(),
                permission.getCreatedAt()
        );
    }
}
