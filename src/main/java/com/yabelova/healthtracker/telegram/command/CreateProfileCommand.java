package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CreateProfileCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileMenuScreen profileMenuScreen;
    private final ReplySender reply;

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.PROFILE_CREATE);
    }

    @Override
    public Object handlePendingText(Update update, User user, Object marker) {
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            reply.send(user, BotTexts.CREATE_PROFILE_NAME_EMPTY);
            return marker; // продолжаем ждать ввод
        }

        Profile saved = profileService.createProfile(user, name);

        reply.send(user, BotTexts.CREATE_PROFILE_SUCCESS.formatted(HtmlUtils.bold(saved.getName())), ParseMode.HTML);

        profileMenuScreen.render(user);
        return null;
    }

    @Override
    public Object handleCallback(Update update, User user) {
        reply.send(user, BotTexts.CREATE_PROFILE_PROMPT);

        return CallbackAction.PROFILE_CREATE; // ожидаем следующий текст (имя)
    }
}
