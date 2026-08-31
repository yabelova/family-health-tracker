package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AddProfileCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileSelectionScreen profileSelectionScreen;

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.PROFILE_ADD);
    }

    @Override
    public Object handlePendingText(Update update, User user, ReplySender reply, Object marker) {
        String raw = update.getMessage().getText();

        if (raw == null || raw.trim().isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.ADD_CODE_PROMPT)
                    .build());
            return marker;
        }

        try {
            Profile profile = profileService.claimInvite(user, raw);

            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.ADD_PROFILE_SUCCESS.formatted(HtmlUtils.bold(profile.getName())))
                    .parseMode("HTML")
                    .build());

            profileSelectionScreen.render(user, reply);
            return null;

        } catch (ProfileOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(e.getMessage())
                    .build());
            return marker; // продолжаем ждать корректный код
        }
    }

    @Override
    public Object handleCallback(Update update, User user, ReplySender reply) {
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.ADD_CODE_PROMPT)
                .build());

        return CallbackAction.PROFILE_ADD; // ожидаем следующий текст (код)
    }
}