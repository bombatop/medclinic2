package com.medclinic.notification.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medclinic.notification.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramApiClient {

    private static final int MAX_ATTEMPTS = 3;

    private final TelegramProperties telegramProperties;
    private final ObjectMapper objectMapper;

    /**
     * Sends a text message. Skips when bot token is blank. Returns false on failure after retries.
     */
    public boolean sendMessage(String chatId, String text) {
        String token = telegramProperties.getBotToken();
        if (token == null || token.isBlank()) {
            log.warn("Telegram bot token not configured; skip sendMessage");
            return false;
        }
        if (chatId == null || chatId.isBlank()) {
            log.warn("Telegram chat id blank; skip sendMessage");
            return false;
        }

        RestClient client = RestClient.create();
        String base = "https://api.telegram.org/bot" + token.trim() + "/sendMessage";

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String body = client.post()
                        .uri(base)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("chat_id", chatId.trim(), "text", text))
                        .retrieve()
                        .body(String.class);
                if (body == null) {
                    log.error("Telegram empty response");
                    return false;
                }
                JsonNode root = objectMapper.readTree(body);
                if (root.path("ok").asBoolean(false)) {
                    return true;
                }
                int errorCode = root.path("error_code").asInt(0);
                if (errorCode == 429 && attempt < MAX_ATTEMPTS) {
                    int retryAfter = root.path("parameters").path("retry_after").asInt(1);
                    log.warn("Telegram rate limit; retry after {}s (attempt {})", retryAfter, attempt);
                    Thread.sleep(Math.min(60_000L, retryAfter * 1000L));
                    continue;
                }
                log.error("Telegram API error: {}", root.path("description").asText("unknown"));
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Telegram send interrupted");
                return false;
            } catch (Exception e) {
                log.error("Telegram send failed (attempt {}): {}", attempt, e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    return false;
                }
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}
