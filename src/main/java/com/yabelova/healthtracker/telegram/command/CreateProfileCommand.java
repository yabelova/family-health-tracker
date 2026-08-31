package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
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
public class CreateProfileCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileMenuScreen profileMenuScreen;

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.CREATE_PROFILE);
    }

    @Override
    public Object handlePendingText(Update update, User user, ReplySender reply, Object marker) {
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.CREATE_PROFILE_NAME_EMPTY)
                    .build());
            return marker; // продолжаем ждать ввод
        }

        Profile saved = profileService.createProfile(user, name);

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.CREATE_PROFILE_SUCCESS.formatted(HtmlUtils.bold(saved.getName())))
                .parseMode("HTML")
                .build());

        profileMenuScreen.render(user, reply);
        return null;
    }

    @Override
    public Object handleCallback(Update update, User user, ReplySender reply) {
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.CREATE_PROFILE_PROMPT)
                .build());

        return CallbackAction.CREATE_PROFILE; // ожидаем следующий текст (имя)
    }
}
