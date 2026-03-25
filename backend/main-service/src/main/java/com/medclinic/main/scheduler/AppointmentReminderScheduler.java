package com.medclinic.main.scheduler;

import com.medclinic.main.config.ReminderProperties;
import com.medclinic.main.model.AppointmentReminderKind;
import com.medclinic.main.model.AppointmentStatus;
import com.medclinic.main.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentReminderDispatchService dispatchService;
    private final ReminderProperties reminderProperties;

    @Scheduled(fixedDelayString = "${medclinic.reminders.poll-interval-ms:120000}")
    public void dispatchDueReminders() {
        try {
            for (AppointmentReminderKind kind : AppointmentReminderKind.values()) {
                dispatch(kind);
            }
        } catch (Exception e) {
            log.error("Reminder scheduler failed", e);
        }
    }

    private void dispatch(AppointmentReminderKind kind) {
        if (!windowEnabled(kind.eventType())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int half = reminderProperties.getWindowHalfWidthMinutes();
        LocalDateTime winStart = now.plusHours(kind.getHoursBeforeVisit()).minusMinutes(half);
        LocalDateTime winEnd = now.plusHours(kind.getHoursBeforeVisit()).plusMinutes(half);
        List<Long> ids = appointmentRepository.findIdsEligibleForReminder(
                AppointmentStatus.SCHEDULED, winStart, winEnd, kind.getQueryKey());
        for (Long id : ids) {
            dispatchService.tryClaimAndPublish(id, winStart, winEnd, kind);
        }
    }

    private boolean windowEnabled(String code) {
        Set<String> upper = reminderProperties.getWindows().stream()
                .map(w -> w == null ? "" : w.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return upper.contains(code.toUpperCase(Locale.ROOT));
    }
}
