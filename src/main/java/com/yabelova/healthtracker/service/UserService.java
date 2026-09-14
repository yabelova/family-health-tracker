package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public User getOrCreate(Long telegramId, String telegramFirstName, String telegramUsername) {
        User user = userRepository.findByTelegramId(telegramId).orElseGet(() ->
                userRepository.save(User.builder()
                        .telegramId(telegramId)
                        .telegramFirstName(telegramFirstName)
                        .telegramUsername(telegramUsername)
                        .createdAt(Instant.now())
                        .build())
        );
        user.setActiveProfileId(profileRepository.findActiveProfileIdByUserId(user.getId()));
        return user;
    }
}
