package com.yabelova.healthtracker.telegram;

import com.yabelova.healthtracker.config.TelegramBotConfig;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
public class HealthTrackerBot extends TelegramLongPollingBot {

    private final TelegramBotConfig botConfig;
    private final BotDispatcher dispatcher;

    public HealthTrackerBot(TelegramBotConfig botConfig, BotDispatcher dispatcher) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.dispatcher = dispatcher;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            dispatcher.dispatch(update);
        } catch (Exception e) {
            log.error("Ошибка обработки Update: ", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }
}
