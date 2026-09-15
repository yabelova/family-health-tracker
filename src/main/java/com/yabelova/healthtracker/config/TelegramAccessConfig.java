package com.yabelova.healthtracker.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "telegram.access")
@Data
@Slf4j
public class TelegramAccessConfig {

    private List<Long> allowedIds = List.of();

    public boolean isAllowed(Long telegramId) {
        return allowedIds.contains(telegramId);
    }

    @PostConstruct
    void warnIfClosed() {
        if (allowedIds.isEmpty()) {
            log.warn("TELEGRAM_ALLOWED_IDS не задан — бот полностью закрыт");
        }
    }
}