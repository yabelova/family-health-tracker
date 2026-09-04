package com.yabelova.healthtracker.telegram.command.section;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.command.BotCommand;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.screen.RecordSectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.RecordDeleteOption;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Общая команда раздела записей (симптомы / курсы лечения). Реализует рендер
 * меню раздела, выгрузку и удаление по id.
 * <p>
 * Конкретный раздел наследует этот класс и задаёт только: callback-действия, чтение/удаление и форматирование.
 */
public abstract class AbstractSectionCommand<E> implements BotCommand {

    protected final ProfileService profileService;
    protected final RecordSectionScreen sectionScreen;
    private final ProfileSelectionScreen profileSelectionScreen;

    protected AbstractSectionCommand(ProfileService profileService,
                                     RecordSectionScreen sectionScreen,
                                     ProfileSelectionScreen profileSelectionScreen) {
        this.profileService = profileService;
        this.sectionScreen = sectionScreen;
        this.profileSelectionScreen = profileSelectionScreen;
    }

    /**
     * Callback: открыть меню раздела.
     */
    protected abstract CallbackAction openAction();

    /**
     * Callback: кнопка «Добавить» (ведёт на визард-команду этого домена).
     */
    protected abstract CallbackAction addAction();

    /**
     * Callback: выгрузка записей.
     */
    protected abstract CallbackAction exportAction();

    /**
     * Callback: список записей для удаления.
     */
    protected abstract CallbackAction deleteAction();

    /**
     * Callback: удалить запись по id (data = deleteSelectedAction:id).
     */
    protected abstract CallbackAction deleteSelectedAction();

    /**
     * Заголовок меню раздела.
     */
    protected abstract String sectionTitle();

    /**
     * Записи для выгрузки.
     */
    protected abstract List<E> listForExport(Long userId, Integer profileId);

    /**
     * Записи для списка удаления (все).
     */
    protected abstract List<E> listForDelete(Long userId, Integer profileId);

    /**
     * id записи.
     */
    protected abstract Integer idOf(E record);

    /**
     * Короткая подпись кнопки удаления.
     */
    protected abstract String deleteLabel(E record);

    /**
     * Строка записи в выгрузке.
     */
    protected abstract String formatExport(E record);

    /**
     * Сообщение, если выгружать нечего.
     */
    protected abstract String exportEmpty();

    /**
     * Удалить запись по id (проверяет принадлежность профилю).
     */
    protected abstract void delete(Long userId, Integer profileId, Integer id);

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(openAction(), exportAction(), deleteAction(), deleteSelectedAction());
    }

    @Override
    public final Object handleCallback(Update update, User user, ReplySender reply, Object marker) {
        CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            return null;
        }

        if (action == openAction()) {
            showSection(user, reply);
            return null;
        }

        try {
            if (action == exportAction()) {
                renderExport(user, reply, profile);
                return null;
            }
            if (action == deleteAction()) {
                renderDeleteList(user, reply, profile);
                return null;
            }
            if (action == deleteSelectedAction()) {
                handleDeleteSelected(update, user, reply, profile);
                return null;
            }
        } catch (RecordOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(e.getMessage())
                    .build());
            if (e.getError() == RecordOperationException.Error.PROFILE_ACCESS_DENIED) {
                profileSelectionScreen.render(user, reply);
            } else {
                renderDeleteList(user, reply, profile);
            }
            return null;
        }

        return null;
    }

    /**
     * Показать меню раздела. Вызывается из callback'а открытия и из визарда после добавления.
     */
    public void showSection(User user, ReplySender reply) {
        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            return;
        }
        sectionScreen.renderSection(user, reply, sectionTitle(),
                addAction(), exportAction(), deleteAction());
    }

    private void renderExport(User user, ReplySender reply, Profile profile) {
        List<E> records = listForExport(user.getId(), profile.getId());
        if (records.isEmpty()) {
            sectionScreen.renderExport(user, reply, exportEmpty(), openAction());
            return;
        }
        String head = BotTexts.EXPORT_HEAD.formatted(HtmlUtils.bold(profile.getName()));
        String body = records.stream()
                .map(this::formatExport)
                .collect(Collectors.joining("\n"));
        sectionScreen.renderExport(user, reply, head + "\n\n" + body, openAction());
    }

    private void renderDeleteList(User user, ReplySender reply, Profile profile) {
        List<E> records = listForDelete(user.getId(), profile.getId());
        List<RecordDeleteOption> options = records.stream()
                .map(record -> new RecordDeleteOption(idOf(record), deleteLabel(record)))
                .toList();
        String head = options.isEmpty() ? BotTexts.DELETE_RECORDS_EMPTY : BotTexts.DELETE_LIST_HEAD;
        sectionScreen.renderDeleteList(user, reply, head, options, deleteSelectedAction(), openAction());
    }

    private void handleDeleteSelected(Update update, User user, ReplySender reply, Profile profile) {
        String raw = CallbackAction.payloadOf(update.getCallbackQuery().getData());
        Integer id = parseId(raw);
        if (id == null) {
            return;
        }
        delete(user.getId(), profile.getId(), id);
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.RECORD_DELETED)
                .build());
        renderDeleteList(user, reply, profile);
    }

    private Integer parseId(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
