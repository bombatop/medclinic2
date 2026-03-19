package com.medclinic.auth.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UpdateRolePermissionsRequest(
        Set<@NotBlank String> permissions
) {}
