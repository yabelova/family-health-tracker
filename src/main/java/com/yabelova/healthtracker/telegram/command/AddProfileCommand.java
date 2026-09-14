package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
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
public class AddProfileCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final ReplySender reply;

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.PROFILE_ADD_BY_CODE);
    }

    @Override
    public Object handlePendingText(Update update, User user, Object marker) {
        String raw = update.getMessage().getText();

        if (raw == null || raw.trim().isEmpty()) {
            reply.send(user, BotTexts.ADD_CODE_PROMPT);
            return marker;
        }

        try {
            Profile profile = profileService.claimInvite(user, raw);

            reply.send(user, BotTexts.ADD_PROFILE_SUCCESS.formatted(HtmlUtils.bold(profile.getName())),
                    ParseMode.HTML);

            profileSelectionScreen.render(user);
            return null;

        } catch (ProfileOperationException e) {
            reply.send(user, e.getMessage());
            return marker; // продолжаем ждать корректный код
        }
    }

    @Override
    public Object handleCallback(Update update, User user) {
        reply.answerCallbackQuery(update.getCallbackQuery().getId());

        reply.send(user, BotTexts.ADD_CODE_PROMPT);

        return CallbackAction.PROFILE_ADD_BY_CODE; // ожидаем следующий текст (код)
    }
}
