package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileMenuCommand implements BotCommand {

    private final ProfileMenuScreen profileMenuScreen;
    private final ProfileSelectionScreen profileSelectionScreen;

    @Override
    public Set<String> textKeys() {
        return Set.of(BotTexts.REPLY_BTN_PROFILE_MENU);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.MAIN_MENU_ACTION);
    }

    @Override
    public Object handleText(Update update, User user, ReplySender reply) {
        if (user.getActiveProfileId() == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            profileSelectionScreen.render(user, reply);
            return null;
        }
        profileMenuScreen.render(user, reply);
        return null;
    }

    @Override
    public Object handleCallback(Update update, User user, ReplySender reply) {
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());
        profileMenuScreen.render(user, reply);
        return null;
    }
}
