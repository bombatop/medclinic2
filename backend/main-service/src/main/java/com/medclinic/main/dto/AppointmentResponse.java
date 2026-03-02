package com.medclinic.main.dto;

import com.medclinic.main.model.Appointment;
import com.medclinic.main.model.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long clientId,
        String clientName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String notes,
        Instant createdAt
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getEmployee().getId(),
                a.getEmployee().getFirstName() + " " + a.getEmployee().getLastName(),
                a.getClient().getId(),
                a.getClient().getFirstName() + " " + a.getClient().getLastName(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getNotes(),
                a.getCreatedAt()
        );
    }
}
