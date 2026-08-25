package com.yabelova.healthtracker.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class StartCommand implements BotCommand {
    @Override
    public String getCommandIdentifier() {
        return "/start";
    }

    @Override
    public String getButtonAlias() {
        return "🚀 Запустить трекер";
    }

    @Override
    public SendMessage execute(Update update) {
        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getFrom().getFirstName();

        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Привет, " + firstName + "! Добро пожаловать в семейный трекер здоровья 💚")
                .build();
    }
}
