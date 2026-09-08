package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.NotificationService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.NotificationsScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
    private final ReplySender reply;

    @Override
    public Set<String> textKeys() {
        return Set.of(BotTexts.REPLY_BTN_NOTIFICATIONS);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.NOTIFICATION_EDIT, CallbackAction.NOTIFICATION_DISABLE);
    }

    @Override
    public Object handleText(Update update, User user) {
        notificationsScreen.render(user);
        return null;
    }

    @Override
    public Object handlePendingText(Update update, User user, Object marker) {
        String raw = update.getMessage().getText().trim();

        try {
            LocalTime time = notificationService.setTime(user, raw);
            log.info("Установлено время уведомлений для [{}]: {}", user.getId(), time);

            notificationsScreen.render(user);
            return null;

        } catch (DateTimeParseException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.NOTIFICATIONS_INVALID_FORMAT)
                    .build());
            return marker; // продолжаем ждать корректный ввод
        }
    }

    @Override
    public Object handleCallback(Update update, User user) {
        CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());
        reply.answerCallbackQuery(update.getCallbackQuery().getId());

        if (action == CallbackAction.NOTIFICATION_EDIT) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.NOTIFICATIONS_ENTER_TIME)
                    .build());
            return CallbackAction.NOTIFICATION_EDIT; // ожидаем ввод времени

        } else if (action == CallbackAction.NOTIFICATION_DISABLE) {
            notificationService.disable(user);
            notificationsScreen.render(user);
            return null;
        }

        return null;
    }
}
