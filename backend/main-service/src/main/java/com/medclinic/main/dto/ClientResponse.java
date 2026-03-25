package com.medclinic.main.dto;

import com.medclinic.main.model.Client;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String notes,
        boolean receiveAppointmentReminders,
        String telegramChatId,
        Instant createdAt
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getPhone(),
                client.getEmail(),
                client.getNotes(),
                client.isReceiveAppointmentReminders(),
                client.getTelegramChatId(),
                client.getCreatedAt()
        );
    }
}
