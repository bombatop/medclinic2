package com.medclinic.notification.listener;

import com.medclinic.shared.event.AppointmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class AppointmentEventListener {

    @Bean
    public Consumer<AppointmentEvent> handleAppointmentEvent() {
        return event -> {
            log.info("Received appointment event: type={}, appointmentId={}, client={}, employee={}, time={}-{}",
                    event.eventType(),
                    event.appointmentId(),
                    event.clientName(),
                    event.employeeName(),
                    event.startTime(),
                    event.endTime());

            switch (event.eventType()) {
                case "CREATED" -> notifyAppointmentCreated(event);
                case "STATUS_CANCELLED" -> notifyAppointmentCancelled(event);
                case "STATUS_COMPLETED" -> notifyAppointmentCompleted(event);
                default -> log.debug("No notification for event type: {}", event.eventType());
            }
        };
    }

    private void notifyAppointmentCreated(AppointmentEvent event) {
        log.info("NOTIFICATION: New appointment for {} with {} on {} at {}",
                event.clientName(),
                event.employeeName(),
                event.startTime().toLocalDate(),
                event.startTime().toLocalTime());

        if (event.clientPhone() != null) {
            log.info("  -> Would send SMS to {}", event.clientPhone());
        }
        if (event.clientEmail() != null) {
            log.info("  -> Would send email to {}", event.clientEmail());
        }
    }

    private void notifyAppointmentCancelled(AppointmentEvent event) {
        log.info("NOTIFICATION: Appointment {} cancelled for {}",
                event.appointmentId(), event.clientName());
    }

    private void notifyAppointmentCompleted(AppointmentEvent event) {
        log.info("NOTIFICATION: Appointment {} completed for {}",
                event.appointmentId(), event.clientName());
    }
}
