package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.DailyPlanBuilder;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlan;
import com.yabelova.healthtracker.service.NotificationService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.NotificationsScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.DailyPlanFormatter;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.TimeZones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationsCommand implements BotCommand {

    private final NotificationService notificationService;
    private final DailyPlanBuilder planBuilder;
    private final NotificationsScreen notificationsScreen;
    private final DailyPlanFormatter planFormatter;
    private final KeyboardFactory keyboard;
    private final ReplySender reply;

    @Override
    public Set<String> textKeys() {
        return Set.of(BotTexts.REPLY_BTN_NOTIFICATIONS);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.NOTIFICATION_EDIT,
                CallbackAction.NOTIFICATION_DISABLE,
                CallbackAction.NOTIFICATION_EXPORT,
                CallbackAction.NOTIFICATION_BACK);
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
            reply.send(user, BotTexts.NOTIFICATIONS_INVALID_FORMAT);
            return marker; // продолжаем ждать корректный ввод
        }
    }

    @Override
    public Object handleCallback(Update update, User user) {
        CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());

        if (action == CallbackAction.NOTIFICATION_EDIT) {
            reply.send(user, BotTexts.NOTIFICATIONS_ENTER_TIME);
            return CallbackAction.NOTIFICATION_EDIT; // ожидаем ввод времени

        } else if (action == CallbackAction.NOTIFICATION_DISABLE) {
            notificationService.disable(user);
            notificationsScreen.render(user);
            return null;

        } else if (action == CallbackAction.NOTIFICATION_EXPORT) {
            DailyPlan plan = planBuilder.build(user, LocalDate.now(TimeZones.DEFAULT));
            String text = plan.profiles().isEmpty()
                    ? BotTexts.DAILY_PLAN_EMPTY
                    : planFormatter.format(plan);
            reply.send(user, text, ParseMode.HTML,
                    keyboard.backToSection(CallbackAction.NOTIFICATION_BACK));
            return null; // ручная выгрузка не помечает пользователя «отправленным»

        } else if (action == CallbackAction.NOTIFICATION_BACK) {
            notificationsScreen.render(user);
            return null;
        }

        return null;
    }
}
