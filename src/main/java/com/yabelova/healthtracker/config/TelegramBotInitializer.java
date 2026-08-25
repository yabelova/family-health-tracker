package com.yabelova.healthtracker.config;

import com.yabelova.healthtracker.telegram.HealthTrackerBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotInitializer {

    @Bean
    public TelegramBotsApi telegramBotsApi(HealthTrackerBot healthTrackerBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(healthTrackerBot);
        return botsApi;
    }
}
