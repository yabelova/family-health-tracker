package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SelectProfileCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final ProfileMenuScreen profileMenuScreen;

    @Override
    public Set<String> textKeys() {
        return Set.of(BotTexts.REPLY_BTN_SELECT_PROFILE);
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.ACTIVATE_PROFILE);
    }

    @Override
    public Object handleText(Update update, User user, ReplySender reply) {
        profileSelectionScreen.render(user, reply);
        return null;
    }

    @Override
    public Object handleCallback(Update update, User user, ReplySender reply) {
        String data = update.getCallbackQuery().getData();
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        try {
            Integer profileId = Integer.valueOf(CallbackAction.payloadOf(data));
            profileService.setActiveProfile(user, profileId);
        } catch (RuntimeException e) {
            log.warn("Битые callback-данные выбора профиля [{}] от [{}]", data, user.getId());
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.PROFILE_UNAVAILABLE)
                    .build());
            profileSelectionScreen.render(user, reply);
            return null;
        }

        profileMenuScreen.render(user, reply);
        return null;
    }
}
