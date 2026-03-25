package com.medclinic.main.event;

import com.medclinic.main.model.Appointment;
import com.medclinic.shared.event.AppointmentReminderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderPublisher {

    private final StreamBridge streamBridge;

    public void publish(Appointment appointment, String reminderType) {
        var client = appointment.getClient();
        var employee = appointment.getEmployee();
        String doctorName = employee.getFirstName() + " " + employee.getLastName();
        AppointmentReminderEvent event = new AppointmentReminderEvent(
                appointment.getId(),
                reminderType,
                client.getFirstName() + " " + client.getLastName(),
                client.getTelegramChatId(),
                appointment.getStartTime(),
                doctorName
        );
        streamBridge.send("appointment-reminders", event);
        log.info("Published {} reminder for appointment {}", reminderType, appointment.getId());
    }
}
