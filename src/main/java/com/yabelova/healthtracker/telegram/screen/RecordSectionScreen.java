package com.yabelova.healthtracker.telegram.screen;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.DeletionData;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Экран раздела записей выбранного профиля: только рисует меню раздела
 * (действия с записями профиля), выгрузку и список для удаления.
 */
@Component
@RequiredArgsConstructor
public class RecordSectionScreen {

    private final KeyboardFactory keyboard;
    private final ReplySender reply;

    public void renderSection(User user,
                              String title,
                              String profileName,
                              String preview,
                              String addLabel,
                              CallbackAction add,
                              CallbackAction export,
                              CallbackAction delete) {
        StringBuilder text = new StringBuilder(title)
                .append('\n')
                .append(BotTexts.SECTION_PROFILE_HEADER.formatted(HtmlUtils.bold(profileName)));
        if (preview != null && !preview.isEmpty()) {
            text.append("\n\n").append(preview);
        }
        text.append("\n\n").append(BotTexts.SECTION_ACTION_PROMPT);

        reply.send(user, text.toString(), ParseMode.HTML, keyboard.sectionMenu(addLabel, add, export, delete));
    }

    public void sendTextWithBack(User user, String text, CallbackAction backAction) {
        reply.send(user, text, ParseMode.HTML, keyboard.backToSection(backAction));
    }

    public void renderDeleteList(User user,
                                 String head,
                                 List<DeletionData> deletions,
                                 CallbackAction deleteSelected,
                                 CallbackAction backAction) {
        reply.send(user, head, ParseMode.HTML,
                deletions.isEmpty() ? keyboard.backToSection(backAction)
                        : keyboard.recordDeleteList(deletions, deleteSelected, backAction));
    }
}
