package com.medclinic.shared.event;

import java.time.LocalDateTime;

/**
 * Published when an appointment reminder window is due (before {@link AppointmentEvent} lifecycle events).
 * JSON field names are camelCase for Spring Cloud Stream / Jackson.
 */
public record AppointmentReminderEvent(
        Long appointmentId,
        /** "H24" or "H1" */
        String reminderType,
        String clientName,
        String telegramChatId,
        LocalDateTime startTime,
        String doctorName
) {}
