package com.medclinic.auth.dto;

import com.medclinic.auth.model.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty Set<@NotNull Role> roles
) {}
