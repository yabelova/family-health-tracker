package com.yabelova.healthtracker.telegram.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.List;

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

    public void answerCallbackQuery(String callbackQueryId) {
        send(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .build());
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        send(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .text(text)
                .showAlert(false)
                .build());
    }

    public void removeKeyboard(Long chatId, Integer messageId) {
        try {
            bot.execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboard(List.of())
                            .build())
                    .build());
        } catch (TelegramApiException e) {
            log.error("Ошибка удаления inline-клавиатуры сообщения {}: ", messageId, e);
        }
    }
}
