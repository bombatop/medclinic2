package com.medclinic.main.event;

import com.medclinic.main.model.Appointment;
import com.medclinic.shared.event.AppointmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventPublisher {

    private final StreamBridge streamBridge;

    public void publish(Appointment appointment, String eventType) {
        AppointmentEvent event = new AppointmentEvent(
                appointment.getId(),
                eventType,
                appointment.getEmployee().getId(),
                appointment.getEmployee().getFirstName() + " " + appointment.getEmployee().getLastName(),
                appointment.getClient().getId(),
                appointment.getClient().getFirstName() + " " + appointment.getClient().getLastName(),
                appointment.getClient().getPhone(),
                appointment.getClient().getEmail(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus().name()
        );

        streamBridge.send("appointment-events", event);
        log.info("Published {} event for appointment {}", eventType, appointment.getId());
    }
}
