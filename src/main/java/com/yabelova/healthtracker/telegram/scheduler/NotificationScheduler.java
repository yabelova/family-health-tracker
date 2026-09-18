package com.yabelova.healthtracker.telegram.scheduler;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.DailyPlanBuilder;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlan;
import com.yabelova.healthtracker.service.NotificationService;
import com.yabelova.healthtracker.telegram.support.DailyPlanFormatter;
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.TimeZones;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Ежедневная рассылка планов на сегодня с ежеминутной проверкой времени.
 * Догон при пропущенном тике обеспечивает признак {@code last_notified_date}: неотправленные в свой момент
 * пользователи попадут в следующий запуск (время уже наступило, пометки нет). Сбой доставки одному пользователю
 * не влияет на остальных: пометку не ставим, следующий тик догонит.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final DailyPlanBuilder planBuilder;
    private final DailyPlanFormatter planFormatter;
    private final ReplySender reply;

    @Scheduled(cron = "0 * * * * *")
    public void sendDailyPlans() {
        sendDailyPlans(LocalDateTime.now(TimeZones.DEFAULT));
    }

    void sendDailyPlans(LocalDateTime now) {
        List<User> users;
        try {
            users = notificationService.planRecipients(now);
        } catch (Exception e) {
                log.error("Ошибка выборки получателей ежедневной рассылки: ", e);
            return;
        }
        if (users.isEmpty()) {
            return;
        }

        LocalDate today = now.toLocalDate();
        Map<Integer, DailyPlan> plans;
        try {
            plans = planBuilder.buildAll(users.stream().map(User::getId).toList(), today);
        } catch (Exception e) {
            log.error("Ошибка сборки планов: ", e);
            return;
        }

        for (User user : users) {
            try {
                sendPlanTo(user, today, plans.getOrDefault(user.getId(), DailyPlanBuilder.EMPTY));
            } catch (Exception e) {
                log.error("Ошибка ежедневной рассылки для userId={}: ", user.getId(), e);
            }
        }
    }

    private void sendPlanTo(User user, LocalDate today, DailyPlan plan) {
        if (!plan.profiles().isEmpty()) {
            if (!reply.trySend(user, planFormatter.format(plan), ParseMode.HTML)) {
                // доставка не подтверждена -> пометку не ставим, следующий тик напомнит
                return;
            }
        }
        // пустой день: молчим, но помечаем отправку - иначе ретрай каждую минуту
        notificationService.markNotified(user, today);
    }
}
