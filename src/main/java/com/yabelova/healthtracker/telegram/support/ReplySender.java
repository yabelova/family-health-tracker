package com.yabelova.healthtracker.telegram.support;

import com.yabelova.healthtracker.domain.User;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
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

    public void send(User user, String text) {
        sendInternal(user, text, null, null);
    }

    public void send(Long chatId, String text) {
        send(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build());
    }

    public void send(User user, String text, ParseMode parseMode) {
        sendInternal(user, text, parseMode, null);
    }

    public void send(User user, String text, ReplyKeyboard replyMarkup) {
        sendInternal(user, text, null, replyMarkup);
    }

    public void send(User user, String text, ParseMode parseMode, ReplyKeyboard replyMarkup) {
        sendInternal(user, text, parseMode, replyMarkup);
    }

    public void answerCallbackQuery(String callbackQueryId) {
        send(AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
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

    private void sendInternal(User user, String text, @Nullable ParseMode parseMode, @Nullable ReplyKeyboard replyMarkup) {
        send(SendMessage.builder()
                .chatId(user.getTelegramId().toString())
                .text(text)
                .parseMode(parseMode == null ? null : parseMode.name())
                .replyMarkup(replyMarkup)
                .build());
    }

    private <T extends Serializable> void send(BotApiMethod<T> method) {
        try {
            bot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки Telegram-метода {}: ", method.getClass().getSimpleName(), e);
        }
    }
}
