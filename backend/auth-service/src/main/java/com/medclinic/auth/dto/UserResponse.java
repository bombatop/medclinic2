package com.medclinic.auth.dto;

import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
