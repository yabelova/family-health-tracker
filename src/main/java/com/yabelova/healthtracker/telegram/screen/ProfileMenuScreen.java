package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/**
 * Общий экран «Меню профиля». Вызывается из Start, Select, Create и той же reply-кнопки.
 */
@Component
@RequiredArgsConstructor
public class ProfileMenuScreen {

    private final ProfileService profileService;
    private final KeyboardFactory keyboard;

    public void render(User user, ReplySender reply) {
        Profile profile = profileService.getActiveProfile(user);

        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("⚠️ Профиль не существует. Выберите другой")
                    .replyMarkup(keyboard.navigation(false))
                    .build());
            return;
        }

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text("👤 Профиль: " + HtmlUtils.bold(profile.getName()))
                .parseMode("HTML")
                .replyMarkup(keyboard.navigation(true))
                .build());

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text("Выберите действие:")
                .replyMarkup(keyboard.profileMenu())
                .build());
    }
}
