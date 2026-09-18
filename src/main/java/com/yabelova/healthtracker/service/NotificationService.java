package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.UserRepository;
import com.yabelova.healthtracker.util.Dates;
import com.yabelova.healthtracker.util.TimeZones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;

    public LocalTime setTime(User user, String raw) {
        return setTimeInternal(user, raw, LocalDateTime.now(TimeZones.DEFAULT));
    }

    /**
     * Установка времени напоминания.
     * Момент {@code now} - точка отсчета для правила: если время в будущем - напоминание придет сегодня,
     * если уже наступившее - сегодня не шлем, считаем отправленным.
     */
    LocalTime setTimeInternal(User user, String raw, LocalDateTime now) {
        LocalTime time = LocalTime.parse(raw, Dates.TIME);
        LocalDate lastNotified = time.isAfter(now.toLocalTime()) ? null : now.toLocalDate();

        userRepository.updateNotificationTime(user.getId(), time, lastNotified);
        user.setNotificationTime(time);
        user.setLastNotifiedDate(lastNotified);
        return time;
    }

    /**
     * Отключение ежедневной рассылки.
     */
    public void disable(User user) {
        userRepository.clearNotificationTime(user.getId());
        user.setNotificationTime(null);
    }

    /**
     * Пользователи, которым положена рассылка плана в момент {@code now}: время наступило, а на день {@code now}
     * план еще не отправлен.
     */
    public List<User> planRecipients(LocalDateTime now) {
        return userRepository.findPlanRecipients(now);
    }

    /**
     * Пометить пользователя «план на {@code day} отправлен» (дедуп в пределах дня).
     */
    public void markNotified(User user, LocalDate day) {
        userRepository.markNotified(user.getId(), day);
        user.setLastNotifiedDate(day);
    }
}
