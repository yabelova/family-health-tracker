package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileMenuCommand implements BotCommand {

    private final ProfileMenuScreen profileMenuScreen;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final ReplySender reply;

    @Override
    public Set<String> textKeys() {
        return Set.of(BotTexts.REPLY_BTN_PROFILE_MENU);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.MAIN_MENU_ACTION);
    }

    @Override
    public Object handleText(Update update, User user) {
        if (user.getActiveProfileId() == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            profileSelectionScreen.render(user);
            return null;
        }
        profileMenuScreen.render(user);
        return null;
    }

    @Override
    public Object handleCallback(Update update, User user) {
        profileMenuScreen.render(user);
        return null;
    }
}
