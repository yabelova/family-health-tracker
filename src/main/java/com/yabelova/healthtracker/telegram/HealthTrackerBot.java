package com.yabelova.healthtracker.telegram;

import com.yabelova.healthtracker.config.TelegramBotConfig;
import com.yabelova.healthtracker.telegram.command.BotCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class HealthTrackerBot extends TelegramLongPollingBot {

    private final TelegramBotConfig botConfig;
    private final Map<String, BotCommand> commands;

    public HealthTrackerBot(TelegramBotConfig botConfig, List<BotCommand> botCommands) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.commands = new HashMap<>();
        for (BotCommand cmd : botCommands) {
            this.commands.put(cmd.getCommandIdentifier(), cmd);

            if (cmd.getButtonAlias() != null) {
                this.commands.put(cmd.getButtonAlias(), cmd);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            log.info("Текст из чата: [{}] от ID: [{}]", text, chatId);

            BotCommand command = commands.get(text);

            if (command != null) {
                try {
                    SendMessage response = command.execute(update);
                    execute(response);
                } catch (TelegramApiException e) {
                    log.error("Ошибка отправки: ", e);
                }
            } else {
                String availableCommands = commands.keySet().stream()
                        .filter(key -> key.startsWith("/"))
                        .sorted()
                        .collect(Collectors.joining(", "));
                sendSimpleText(chatId, "Неизвестная команда\nПожалуйста, используйте кнопки меню или быстрые команды: [" + availableCommands + "]");
            }
        }
    }

    private void sendSimpleText(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId.toString()).text(text).build());
        } catch (TelegramApiException e) {
            log.error("Ошибка дефолтной отправки: ", e);
        }
    }
}
