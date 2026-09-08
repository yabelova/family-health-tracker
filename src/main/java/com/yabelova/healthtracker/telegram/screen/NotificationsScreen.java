package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.BotTexts;
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
    private final ReplySender reply;

    public void render(User user) {
        String time = user.getNotificationTime() != null
                ? user.getNotificationTime().toString()
                : BotTexts.NOTIFICATIONS_TIME_UNSET;

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.NOTIFICATIONS_CURRENT.formatted(HtmlUtils.bold(time)))
                .parseMode("HTML")
                .replyMarkup(keyboard.notifications(user.getNotificationTime() != null))
                .build());
    }
}
