package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    private final UserRepository userRepository;

    public LocalTime setTime(User user, String raw) {
        LocalTime time = LocalTime.parse(raw, TIME_FORMATTER);
        user.setNotificationTime(time);
        userRepository.save(user);
        return time;
    }

    public void disable(User user) {
        user.setNotificationTime(null);
        userRepository.save(user);
    }
}