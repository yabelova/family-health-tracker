package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public User getOrCreate(Long chatId, String firstName, String userName) {
        User user = userRepository.findById(chatId).orElseGet(() ->
                userRepository.save(User.builder()
                        .id(chatId)
                        .firstName(firstName)
                        .username(userName)
                        .createdAt(Instant.now())
                        .isNewEntry(true)
                        .build())
        );
        user.setActiveProfileId(profileRepository.findActiveProfileIdByUserId(chatId));
        return user;
    }
}