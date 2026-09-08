package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.Commands;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class StartCommand implements BotCommand {

    private final KeyboardFactory keyboard;
    private final ReplySender reply;

    @Override
    public Set<String> textKeys() {
        return Set.of(Commands.START.token());
    }

    @Override
    public Object handleText(Update update, User user) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.DISCLAIMER)
                .replyMarkup(keyboard.navigation())
                .build());
        return null;
    }
}
