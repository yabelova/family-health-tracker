package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.BotCommand;
import com.yabelova.healthtracker.telegram.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileMenuCommand implements BotCommand {

    private final ProfileMenuScreen profileMenuScreen;

    @Override
    public Set<String> textKeys() {
        return Set.of(KeyboardFactory.REPLY_BTN_PROFILE_MENU);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.MAIN_MENU_ACTION);
    }

    @Override
    public Object handleText(Update update, User user, ReplySender reply) {
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
