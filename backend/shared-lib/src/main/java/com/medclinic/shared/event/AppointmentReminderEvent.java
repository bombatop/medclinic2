package com.medclinic.shared.event;

import java.time.LocalDateTime;

/**
 * Published when an appointment reminder window is due (1h or 24h before visit).
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
