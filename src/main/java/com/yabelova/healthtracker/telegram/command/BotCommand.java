package com.yabelova.healthtracker.telegram.command;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface BotCommand {

    SendMessage execute(Update update);

    String getCommandIdentifier();

    default String getButtonAlias() {
        return null;
    }
}
