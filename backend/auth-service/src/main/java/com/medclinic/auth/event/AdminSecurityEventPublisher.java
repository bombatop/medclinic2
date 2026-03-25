package com.medclinic.auth.event;

import com.medclinic.shared.event.AdminSecurityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSecurityEventPublisher {

    private final StreamBridge streamBridge;

    public void publish(AdminSecurityEvent event) {
        streamBridge.send("admin-security-events", event);
        log.debug("Published admin security event {}", event.eventType());
    }
}
