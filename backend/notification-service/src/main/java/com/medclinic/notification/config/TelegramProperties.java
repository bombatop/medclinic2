package com.medclinic.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "medclinic.telegram")
public class TelegramProperties {

    private String botToken = "";

    /** Single ops chat or user id for admin/security alerts. */
    private String adminChatId = "";
}
