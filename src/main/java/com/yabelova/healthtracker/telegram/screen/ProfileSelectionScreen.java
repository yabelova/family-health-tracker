package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

/**
 * Общий экран «Выбор профиля» (доступен всегда). Показывает список профилей
 * пользователя и возможность создать новый
 */
@Component
@RequiredArgsConstructor
public class ProfileSelectionScreen {

    private final ProfileService profileService;
    private final KeyboardFactory keyboard;
    private final ReplySender reply;

    public void render(User user) {
        List<Profile> profiles = profileService.getProfiles(user);

        String text = profiles.isEmpty()
                ? BotTexts.PROFILE_SELECTION_EMPTY
                : BotTexts.PROFILE_SELECTION_HAS;

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(text)
                .replyMarkup(keyboard.profileSelection(profiles, user.getActiveProfileId()))
                .build());
    }
}
