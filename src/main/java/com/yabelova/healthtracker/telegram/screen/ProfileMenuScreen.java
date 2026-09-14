package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Общий экран «Меню профиля». Вызывается из Start, Select, Create и той же reply-кнопки
 */
@Component
@RequiredArgsConstructor
public class ProfileMenuScreen {

    private final ProfileService profileService;
    private final KeyboardFactory keyboard;
    private final ReplySender reply;

    public void render(User user) {
        Profile profile = profileService.getActiveProfile(user);

        if (profile == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            return;
        }

        boolean isOwner = profileService.isOwner(user, profile.getId());

        reply.send(user, BotTexts.PROFILE_MENU_TITLE.formatted(HtmlUtils.bold(profile.getName())), ParseMode.HTML,
                keyboard.profileMenu(isOwner));
    }
}
