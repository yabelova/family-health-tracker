package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.NotificationService;
import com.yabelova.healthtracker.telegram.BotCommand;
import com.yabelova.healthtracker.telegram.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.NotificationsScreen;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationsCommand implements BotCommand {

    private final NotificationService notificationService;
    private final NotificationsScreen notificationsScreen;

    @Override
    public Set<String> textKeys() {
        return Set.of(KeyboardFactory.REPLY_BTN_NOTIFICATIONS);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.NOTIFICATION_EDIT, CallbackAction.NOTIFICATION_DISABLE);
    }

    @Override
    public boolean handleText(Update update, User user, ReplySender reply) {
        notificationsScreen.render(user, reply);
        return false;
    }

    @Override
    public boolean handlePendingText(Update update, User user, ReplySender reply) {
        String raw = update.getMessage().getText().trim();

        try {
            LocalTime time = notificationService.setTime(user, raw);
            log.info("Установлено время уведомлений для [{}]: {}", user.getId(), time);

            notificationsScreen.render(user, reply);
            return false;

        } catch (DateTimeParseException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("⚠️ Неверный формат! Введите время в формате Ч:ММ (например, 8:30 или 08:30):")
                    .build());
            return true; // продолжаем ждать корректный ввод
        }
    }

    @Override
    public boolean handleCallback(Update update, User user, ReplySender reply) {
        CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        if (action == CallbackAction.NOTIFICATION_EDIT) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("Введите время для ежедневных уведомлений в формате Ч:ММ (например, 8:30 или 08:30):")
                    .build());
            return true; // ожидаем ввод времени

        } else if (action == CallbackAction.NOTIFICATION_DISABLE) {
            notificationService.disable(user);
            notificationsScreen.render(user, reply);
            return false;
        }

        return false;
    }
}
