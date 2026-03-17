package com.medclinic.auth.dto;

import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.User;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phone,
        Set<Role> roles,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        Set<Role> roles = user.getEffectiveRoles();
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                roles,
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
