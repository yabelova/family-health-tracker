package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.UserRepository;
import com.yabelova.healthtracker.util.Dates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;

    public LocalTime setTime(User user, String raw) {
        LocalTime time = LocalTime.parse(raw, Dates.TIME);
        user.setNotificationTime(time);
        userRepository.save(user);
        return time;
    }

    public void disable(User user) {
        user.setNotificationTime(null);
        userRepository.save(user);
    }
}
