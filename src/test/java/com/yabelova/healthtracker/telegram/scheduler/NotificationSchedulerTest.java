package com.yabelova.healthtracker.telegram.scheduler;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.DailyPlanBuilder;
import com.yabelova.healthtracker.service.DailyPlanBuilder.CourseLine;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlan;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlanProfile;
import com.yabelova.healthtracker.service.NotificationService;
import com.yabelova.healthtracker.telegram.support.DailyPlanFormatter;
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты ежедневной рассылки: сборка планов, тишина при пустых планах и догон после сбоя.
 */
class NotificationSchedulerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final DailyPlanBuilder planBuilder = mock(DailyPlanBuilder.class);
    private final DailyPlanFormatter formatter = mock(DailyPlanFormatter.class);
    private final ReplySender reply = mock(ReplySender.class);
    private final NotificationScheduler scheduler =
            new NotificationScheduler(notificationService, planBuilder, formatter, reply);

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 9, 18, 8, 0);
    private static final LocalDate TODAY = FIXED_NOW.toLocalDate();

    @Test
    void sendsPlanAndMarksEachDueUser() {
        User dad = User.builder().id(1).telegramId(10L).telegramFirstName("Папа").build();
        User mom = User.builder().id(2).telegramId(20L).telegramFirstName("Мама").build();
        when(notificationService.planRecipients(any(LocalDateTime.class)))
                .thenReturn(List.of(dad, mom));
        DailyPlan dadPlan = new DailyPlan(
                List.of(new DailyPlanProfile("Аня", List.of(new CourseLine("Антибиотик", 2)))));
        when(planBuilder.buildAll(List.of(1, 2), TODAY))
                .thenReturn(Map.of(1, dadPlan, 2, new DailyPlan(List.of())));
        when(formatter.format(dadPlan)).thenReturn("текст плана");
        when(reply.trySend(eq(dad), eq("текст плана"), eq(ParseMode.HTML))).thenReturn(true);

        scheduler.sendDailyPlans(FIXED_NOW);

        verify(reply, times(1)).trySend(eq(dad), eq("текст плана"), eq(ParseMode.HTML));
        verify(notificationService).markNotified(dad, TODAY);
        verify(notificationService).markNotified(mom, TODAY);
    }

    @Test
    void emptyPlanStaysSilentButIsMarked() {
        User mom = User.builder().id(1).telegramId(10L).telegramFirstName("Мама").build();
        when(notificationService.planRecipients(any(LocalDateTime.class)))
                .thenReturn(List.of(mom));
        when(planBuilder.buildAll(List.of(1), TODAY)).thenReturn(Map.of());

        scheduler.sendDailyPlans(FIXED_NOW);

        verify(reply, never()).trySend(any(User.class), any(String.class), any(ParseMode.class));
        verify(notificationService).markNotified(mom, TODAY);
    }

    @Test
    void undeliveredPlanIsNotMarkedSoNextTickRetries() {
        User dad = User.builder().id(1).telegramId(10L).telegramFirstName("Папа").build();
        when(notificationService.planRecipients(any(LocalDateTime.class)))
                .thenReturn(List.of(dad));
        DailyPlan dadPlan = new DailyPlan(
                List.of(new DailyPlanProfile("Аня", List.of(new CourseLine("Антибиотик", 2)))));
        when(planBuilder.buildAll(List.of(1), TODAY)).thenReturn(Map.of(1, dadPlan));
        when(formatter.format(dadPlan)).thenReturn("текст плана");
        when(reply.trySend(any(User.class), any(String.class), any(ParseMode.class))).thenReturn(false);

        scheduler.sendDailyPlans(FIXED_NOW);

        verify(notificationService, never()).markNotified(any(User.class), any(LocalDate.class));
    }

    @Test
    void failureForOneUserDoesNotStopOthers() {
        User dad = User.builder().id(1).telegramId(10L).telegramFirstName("Папа").build();
        User mom = User.builder().id(2).telegramId(20L).telegramFirstName("Мама").build();
        when(notificationService.planRecipients(any(LocalDateTime.class)))
                .thenReturn(List.of(dad, mom));
        DailyPlan dadPlan = new DailyPlan(
                List.of(new DailyPlanProfile("Аня", List.of(new CourseLine("Антибиотик", 2)))));
        when(planBuilder.buildAll(List.of(1, 2), TODAY))
                .thenReturn(Map.of(1, dadPlan, 2, new DailyPlan(List.of())));
        when(formatter.format(dadPlan)).thenThrow(new RuntimeException("сеть упала"));

        assertThatCode(() -> scheduler.sendDailyPlans(FIXED_NOW)).doesNotThrowAnyException();

        verify(notificationService, never()).markNotified(dad, TODAY);
        verify(notificationService).markNotified(mom, TODAY);
    }

    @Test
    void tickSelectionFailureIsCaughtAndNothingIsMarked() {
        when(notificationService.planRecipients(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("бд упала"));

        assertThatCode(() -> scheduler.sendDailyPlans(FIXED_NOW)).doesNotThrowAnyException();
        verify(notificationService, never()).markNotified(any(User.class), any(LocalDate.class));
    }

    @Test
    void planBuildFailureSkipsTickAndNothingIsMarked() {
        User dad = User.builder().id(1).telegramId(10L).telegramFirstName("Папа").build();
        when(notificationService.planRecipients(any(LocalDateTime.class))).thenReturn(List.of(dad));
        when(planBuilder.buildAll(any(), any())).thenThrow(new RuntimeException("бд упала"));

        assertThatCode(() -> scheduler.sendDailyPlans(FIXED_NOW)).doesNotThrowAnyException();
        verify(notificationService, never()).markNotified(any(User.class), any(LocalDate.class));
    }

    @Test
    void noDueUsersSkipsPlanBuildCompletely() {
        when(notificationService.planRecipients(any(LocalDateTime.class))).thenReturn(List.of());

        scheduler.sendDailyPlans(FIXED_NOW);

        verify(planBuilder, never()).buildAll(any(), any());
    }
}
