package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.RecordDeleteOption;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

/**
 * Тонкий экран раздела записей: только рисует меню раздела, выгрузку и список
 * для удаления. Данные и действия — в команде-разделе и сервисах.
 */
@Component
@RequiredArgsConstructor
public class RecordSectionScreen {

    private final KeyboardFactory keyboard;

    public void renderSection(User user,
                              ReplySender reply,
                              String title,
                              CallbackAction add,
                              CallbackAction export,
                              CallbackAction delete) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(title)
                .parseMode("HTML")
                .replyMarkup(keyboard.sectionMenu(add, export, delete))
                .build());
    }

    public void renderExport(User user, ReplySender reply, String text, CallbackAction backAction) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboard.backToSection(backAction))
                .build());
    }

    public void renderDeleteList(User user,
                                 ReplySender reply,
                                 String head,
                                 List<RecordDeleteOption> options,
                                 CallbackAction deleteSelected,
                                 CallbackAction backAction) {
        SendMessage.SendMessageBuilder message = SendMessage.builder()
                .chatId(user.getId().toString())
                .text(head)
                .parseMode("HTML");
        if (options.isEmpty()) {
            message.replyMarkup(keyboard.backToSection(backAction));
        } else {
            message.replyMarkup(keyboard.recordDeleteList(options, deleteSelected, backAction));
        }
        reply.send(message.build());
    }
}
