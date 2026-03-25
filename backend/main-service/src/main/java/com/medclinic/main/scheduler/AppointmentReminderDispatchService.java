package com.medclinic.main.scheduler;

import com.medclinic.main.event.AppointmentReminderPublisher;
import com.medclinic.main.model.AppointmentReminderKind;
import com.medclinic.main.model.AppointmentStatus;
import com.medclinic.main.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderDispatchService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentReminderPublisher reminderPublisher;

    @Transactional
    public void tryClaimAndPublish(Long id, LocalDateTime winStart, LocalDateTime winEnd, AppointmentReminderKind kind) {
        Instant ts = Instant.now();
        int key = kind.getQueryKey();
        int updated = appointmentRepository.claimReminder(id, ts, AppointmentStatus.SCHEDULED, winStart, winEnd, key);
        if (updated == 1) {
            appointmentRepository.findByIdForReminder(id)
                    .ifPresentOrElse(
                            a -> reminderPublisher.publish(a, kind.eventType()),
                            () -> log.warn("Appointment {} missing after {} claim", id, kind.eventType()));
        }
    }
}
