package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.BotCommand;
import com.yabelova.healthtracker.telegram.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SelectProfileCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final ProfileMenuScreen profileMenuScreen;

    @Override
    public Set<String> textKeys() {
        return Set.of(KeyboardFactory.REPLY_BTN_SELECT_PROFILE);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.ACTIVATE_PROFILE);
    }

    @Override
    public boolean handleText(Update update, User user, ReplySender reply) {
        profileSelectionScreen.render(user, reply);
        return false;
    }

    @Override
    public boolean handlePendingText(Update update, User user, ReplySender reply) {
        return false;
    }

    @Override
    public boolean handleCallback(Update update, User user, ReplySender reply) {
        String data = update.getCallbackQuery().getData();
        Integer profileId = Integer.valueOf(CallbackAction.payloadOf(data));

        profileService.setActiveProfile(user, profileId);

        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        profileMenuScreen.render(user, reply);
        return false;
    }
}
