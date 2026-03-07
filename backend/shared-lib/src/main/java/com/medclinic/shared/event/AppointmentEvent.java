package com.medclinic.shared.event;

import java.time.LocalDateTime;

public record AppointmentEvent(
        Long appointmentId,
        String eventType,
        Long employeeId,
        String employeeName,
        Long clientId,
        String clientName,
        String clientPhone,
        String clientEmail,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {}
