package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Тесты настроек напоминаний: время, отключение рассылки и отметка отправленного плана.
 */
class NotificationServiceUnitTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService service = new NotificationService(userRepository);

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);

    @Test
    void setTimeParsesMskTimeAndSaves() {
        User user = User.builder().id(1).telegramId(10L).telegramFirstName("Мария").build();
        LocalDateTime now = LocalDateTime.of(TODAY, LocalTime.of(8, 0));

        LocalTime time = service.setTimeInternal(user, "8:30", now);

        assertThat(time).isEqualTo(LocalTime.of(8, 30));
        assertThat(user.getNotificationTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(user.getLastNotifiedDate()).isNull();
        verify(userRepository).updateNotificationTime(1, LocalTime.of(8, 30), null);
    }

    @Test
    void setTimeInFutureTodayClearsMark() {
        User user = User.builder().id(1).telegramId(10L).telegramFirstName("Мария")
                .lastNotifiedDate(TODAY).build();
        LocalDateTime now = LocalDateTime.of(TODAY, LocalTime.of(8, 0));

        service.setTimeInternal(user, "21:00", now);

        assertThat(user.getNotificationTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(user.getLastNotifiedDate()).isNull();
        verify(userRepository).updateNotificationTime(1, LocalTime.of(21, 0), null);
    }

    @Test
    void setTimeInPastTodayMarksAsNotified() {
        User user = User.builder().id(1).telegramId(10L).telegramFirstName("Мария").build();
        LocalDateTime now = LocalDateTime.of(TODAY, LocalTime.of(21, 0));

        service.setTimeInternal(user, "8:00", now);

        assertThat(user.getNotificationTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(user.getLastNotifiedDate()).isEqualTo(TODAY);
        verify(userRepository).updateNotificationTime(1, LocalTime.of(8, 0), TODAY);
    }

    @Test
    void setTimeExactlyNowIsTreatedAsPast() {
        User user = User.builder().id(1).telegramId(10L).telegramFirstName("Мария").build();
        LocalDateTime now = LocalDateTime.of(TODAY, LocalTime.of(8, 0));

        service.setTimeInternal(user, "8:00", now);

        assertThat(user.getNotificationTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(user.getLastNotifiedDate()).isEqualTo(TODAY);
        verify(userRepository).updateNotificationTime(1, LocalTime.of(8, 0), TODAY);
    }

    @Test
    void disableClearsTimeViaPointUpdate() {
        User user = User.builder().id(1).telegramId(10L).telegramFirstName("Мария")
                .notificationTime(LocalTime.of(8, 30)).build();

        service.disable(user);

        assertThat(user.getNotificationTime()).isNull();
        verify(userRepository).clearNotificationTime(1);
    }

    @Test
    void markNotifiedSetsTodayAndSaves() {
        User user = User.builder().id(1).telegramId(10L).telegramFirstName("Мария").build();

        service.markNotified(user, TODAY);

        assertThat(user.getLastNotifiedDate()).isEqualTo(TODAY);
        verify(userRepository).markNotified(1, TODAY);
    }

    @Test
    void setTimeRejectsGarbage() {
        User user = User.builder().id(1).telegramId(10L).telegramFirstName("Мария").build();
        LocalDateTime now = LocalDateTime.of(TODAY, LocalTime.of(8, 0));

        assertThatThrownBy(() -> service.setTimeInternal(user, "какой-то текст", now))
                .isInstanceOf(DateTimeParseException.class);
        verify(userRepository, never()).updateNotificationTime(any(), any(), any());
    }
}
