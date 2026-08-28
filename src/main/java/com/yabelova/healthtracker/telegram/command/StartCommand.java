package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.BotCommand;
import com.yabelova.healthtracker.telegram.screen.ProfileMenuScreen;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class StartCommand implements BotCommand {

    private final ProfileMenuScreen profileMenuScreen;
    private final ProfileSelectionScreen profileSelectionScreen;

    @Override
    public Set<String> textKeys() {
        return Set.of("/start");
    }

    @Override
    public Object handleText(Update update, User user, ReplySender reply) {
        if (user.getActiveProfileId() != null) {
            profileMenuScreen.render(user, reply);
        } else {
            profileSelectionScreen.render(user, reply);
        }
        return null;
    }
}
