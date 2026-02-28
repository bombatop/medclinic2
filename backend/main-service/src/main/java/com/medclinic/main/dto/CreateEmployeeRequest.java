package com.medclinic.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmployeeRequest(
        @NotNull Long authUserId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String specialization
) {}
