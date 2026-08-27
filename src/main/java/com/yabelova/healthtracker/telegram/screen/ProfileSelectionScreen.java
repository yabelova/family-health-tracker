package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

/**
 * Общий экран «Выбор профиля» (доступен всегда). Показывает список профилей
 * пользователя и возможность создать новый.
 */
@Component
@RequiredArgsConstructor
public class ProfileSelectionScreen {

    private final ProfileService profileService;
    private final KeyboardFactory keyboard;

    public void render(User user, ReplySender reply) {
        List<Profile> profiles = profileService.getProfiles(user);

        String text = profiles.isEmpty()
                ? "У вас пока нет профилей. Создайте новый 👇"
                : "Выберите активный профиль или создайте новый";

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(text)
                .replyMarkup(keyboard.navigation(user.getActiveProfileId() != null))
                .build());

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text("Ваши профили:")
                .replyMarkup(keyboard.profileSelection(profiles, user.getActiveProfileId()))
                .build());
    }
}
