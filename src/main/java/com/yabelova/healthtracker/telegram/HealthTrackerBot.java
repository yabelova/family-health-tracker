package com.yabelova.healthtracker.telegram;

import com.yabelova.healthtracker.config.TelegramBotConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.BotSession;

@Slf4j
public class HealthTrackerBot extends TelegramLongPollingBot {

    private final TelegramBotConfig botConfig;
    private final UpdateHandler handler;
    private volatile BotSession session;

    public HealthTrackerBot(TelegramBotConfig botConfig, UpdateHandler handler) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.handler = handler;
    }

    public void attachSession(BotSession session) {
        this.session = session;
    }

    @Override
    public void onUpdateReceived(Update update) {
        handler.dispatch(update);
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @PreDestroy
    void stopPolling() {
        if (session != null && session.isRunning()) {
            session.stop();
            log.info("Telegram polling остановлен");
        }
    }
}
