package com.medclinic.notification.listener;

import com.medclinic.shared.event.AdminSecurityEvent;
import com.medclinic.notification.config.TelegramProperties;
import com.medclinic.notification.telegram.TelegramApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminSecurityEventListener {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final TelegramApiClient telegramApiClient;
    private final TelegramProperties telegramProperties;

    @Bean
    public Consumer<AdminSecurityEvent> handleAdminSecurityEvent() {
        return event -> {
            String adminChat = telegramProperties.getAdminChatId();
            if (adminChat == null || adminChat.isBlank()) {
                log.debug("Admin Telegram chat not configured; skip security event {}", event.eventType());
                return;
            }
            String when = event.occurredAt() != null ? TS.format(event.occurredAt()) : "";
            String target = event.targetUsername() != null ? event.targetUsername() : "";
            String role = event.roleCode() != null ? event.roleCode() : "";
            String text = String.format(
                    "[MedClinic] %s\nActor: %s\nTarget user: %s\nRole: %s\nTime: %s\n%s",
                    event.eventType(),
                    event.actorUsername(),
                    target,
                    role,
                    when,
                    event.summary() != null ? event.summary() : ""
            ).trim();
            boolean ok = telegramApiClient.sendMessage(adminChat.trim(), text);
            if (!ok) {
                log.error("Failed to deliver admin security notification for {}", event.eventType());
            }
        };
    }
}
