package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.Commands;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
public class HelpCommand implements BotCommand {

    @Override
    public Set<String> textKeys() {
        return Set.of(Commands.HELP.token(), BotTexts.REPLY_BTN_HELP);
    }

    @Override
    public Object handleText(Update update, User user, ReplySender reply) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.HELP)
                .build());
        return null;
    }
}