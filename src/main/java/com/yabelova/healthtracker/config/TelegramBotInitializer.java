package com.yabelova.healthtracker.config;

import com.yabelova.healthtracker.telegram.BotDispatcher;
import com.yabelova.healthtracker.telegram.HealthTrackerBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotInitializer {

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public HealthTrackerBot healthTrackerBot(TelegramBotConfig botConfig, BotDispatcher dispatcher,
                                             TelegramBotsApi botsApi) throws TelegramApiException {
        HealthTrackerBot bot = new HealthTrackerBot(botConfig, dispatcher);
        botsApi.registerBot(bot);
        return bot;
    }
}
