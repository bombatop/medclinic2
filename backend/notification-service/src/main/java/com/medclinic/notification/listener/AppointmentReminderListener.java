package com.medclinic.notification.listener;

import com.medclinic.shared.event.AppointmentReminderEvent;
import com.medclinic.notification.telegram.TelegramApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppointmentReminderListener {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TelegramApiClient telegramApiClient;

    @Bean
    public Consumer<AppointmentReminderEvent> handleAppointmentReminder() {
        return event -> {
            log.info("Reminder {} for appointment {}", event.reminderType(), event.appointmentId());
            String when = event.startTime() != null ? event.startTime().format(DT) : "?";
            String label = "H24".equals(event.reminderType()) ? "24 hours" : "1 hour";
            String text = String.format(
                    "Reminder (%s before visit): %s with Dr. %s on %s.",
                    label,
                    event.clientName(),
                    event.doctorName(),
                    when
            );
            boolean ok = telegramApiClient.sendMessage(event.telegramChatId(), text);
            if (!ok) {
                log.error("Failed to deliver {} reminder for appointment {}", event.reminderType(), event.appointmentId());
            }
        };
    }
}
