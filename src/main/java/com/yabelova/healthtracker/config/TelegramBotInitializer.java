package com.yabelova.healthtracker.config;

import com.yabelova.healthtracker.telegram.HealthTrackerBot;
import com.yabelova.healthtracker.telegram.UpdateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotInitializer {

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public HealthTrackerBot healthTrackerBot(TelegramBotConfig botConfig, UpdateHandler handler,
                                             TelegramBotsApi botsApi) throws TelegramApiException {
        HealthTrackerBot bot = new HealthTrackerBot(botConfig, handler);
        BotSession session = botsApi.registerBot(bot);
        bot.attachSession(session);
        return bot;
    }
}
