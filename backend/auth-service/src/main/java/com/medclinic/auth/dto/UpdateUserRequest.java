package com.medclinic.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1) String firstName,
        @Size(min = 1) String lastName,
        @Email String email,
        String phone
) {}
