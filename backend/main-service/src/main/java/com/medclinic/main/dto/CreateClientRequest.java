package com.medclinic.main.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        String email,
        String notes,
        Boolean receiveAppointmentReminders,
        String telegramChatId
) {}
