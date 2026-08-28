package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/**
 * Общий экран «Уведомления». Доступен всегда (профиль не требуется)
 */
@Component
@RequiredArgsConstructor
public class NotificationsScreen {

    private final KeyboardFactory keyboard;

    public void render(User user, ReplySender reply) {
        String time = user.getNotificationTime() != null
                ? user.getNotificationTime().toString()
                : "не задано";

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text("🔔 Текущее время напоминаний: " + HtmlUtils.bold(time))
                .parseMode("HTML")
                .replyMarkup(keyboard.navigation(user.getActiveProfileId() != null))
                .build());

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text("Что хотите сделать?\n\n"
                        + "Время вводится в формате Ч:ММ (например, 8:30 или 08:30) — "
                        + "уведомление будет приходить каждый день в это время.")
                .replyMarkup(keyboard.notifications(user.getNotificationTime() != null))
                .build());
    }
}
