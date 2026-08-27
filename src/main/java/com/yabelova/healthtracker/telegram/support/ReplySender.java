package com.yabelova.healthtracker.telegram.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

@Component
@Slf4j
public class ReplySender {

    private final AbsSender bot;

    public ReplySender(@Lazy AbsSender bot) {
        this.bot = bot;
    }

    public <T extends Serializable> void send(BotApiMethod<T> method) {
        try {
            bot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки Telegram-метода {}: ", method.getClass().getSimpleName(), e);
        }
    }
}
