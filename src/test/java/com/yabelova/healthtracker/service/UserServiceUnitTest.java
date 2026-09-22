package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Создание юзера при первом контакте, переиспользование при повторном и дочитывание активного профиля.
 */
class UserServiceUnitTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final UserService service = new UserService(userRepository, profileRepository);

    @Test
    void getOrCreateCreatesNewUserWhenAbsent() {
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(7);
            return user;
        });
        when(profileRepository.findActiveProfileIdByUserId(7)).thenReturn(null);

        User result = service.getOrCreate(100L, "First name", "@username");

        assertThat(result.getId()).isEqualTo(7);
        assertThat(result.getTelegramId()).isEqualTo(100L);
        assertThat(result.getTelegramFirstName()).isEqualTo("First name");
        assertThat(result.getTelegramUsername()).isEqualTo("@username");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getActiveProfileId()).isNull();
    }

    @Test
    void getOrCreateReturnsExistingUserWithActiveProfile() {
        User existing = User.builder().id(7).telegramId(100L).telegramFirstName("First name").build();
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(existing));
        when(profileRepository.findActiveProfileIdByUserId(7)).thenReturn(4);

        User result = service.getOrCreate(100L, "First name", null);

        assertThat(result).isSameAs(existing);
        assertThat(result.getActiveProfileId()).isEqualTo(4);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrCreateReturnsExistingUserWithoutActiveProfile() {
        User existing = User.builder().id(7).telegramId(100L).telegramFirstName("First name").build();
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(existing));
        when(profileRepository.findActiveProfileIdByUserId(7)).thenReturn(null);

        User result = service.getOrCreate(100L, "First name", null);

        assertThat(result).isSameAs(existing);
        assertThat(result.getActiveProfileId()).isNull();
        verify(userRepository, never()).save(any());
    }
}
