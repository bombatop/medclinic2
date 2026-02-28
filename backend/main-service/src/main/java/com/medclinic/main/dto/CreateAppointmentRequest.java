package com.medclinic.main.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(
        @NotNull Long employeeId,
        @NotNull Long clientId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String notes
) {}
