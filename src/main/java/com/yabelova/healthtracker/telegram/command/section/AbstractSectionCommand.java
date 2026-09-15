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
import com.yabelova.healthtracker.telegram.support.DeletionData;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Numbers;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Общая команда раздела записей выбранного профиля (симптомы / курсы лечения / приемы).
 * Раздел — это набор действий с записями профиля: список, добавление, выгрузка, удаление.
 * Класс реализует рендер меню раздела, выгрузку и удаление в контексте активного профиля.
 * <p>
 * Конкретный раздел наследует этот класс и задает только: callback-действия, чтение/удаление и форматирование.
 */
public abstract class AbstractSectionCommand<E> implements BotCommand {

    protected final ProfileService profileService;
    protected final RecordSectionScreen sectionScreen;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final ReplySender reply;

    protected AbstractSectionCommand(ProfileService profileService,
                                     RecordSectionScreen sectionScreen,
                                     ProfileSelectionScreen profileSelectionScreen,
                                     ReplySender reply) {
        this.profileService = profileService;
        this.sectionScreen = sectionScreen;
        this.profileSelectionScreen = profileSelectionScreen;
        this.reply = reply;
    }

    /**
     * Callback: открыть меню раздела.
     */
    protected abstract CallbackAction openAction();

    /**
     * Callback: кнопка «Добавить» (ведет на визард-команду этого домена).
     */
    protected abstract CallbackAction addAction();

    /**
     * Callback: выгрузка записей.
     */
    protected abstract CallbackAction exportAction();

    /**
     * Callback: получить список записей для удаления.
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
     * Дополнительный блок в заголовке раздела.
     */
    protected String sectionPreview(User user, Profile profile) {
        return "";
    }

    /**
     * Подпись кнопки добавления в меню раздела.
     */
    protected String addLabel() {
        return BotTexts.INLINE_BTN_SECTION_ADD;
    }

    /**
     * Записи для выгрузки.
     */
    protected abstract List<E> listForExport(Integer userId, Integer profileId);

    /**
     * Строка записи в выгрузке.
     */
    protected abstract String exportLine(E record);

    /**
     * Сообщение, если выгружать нечего.
     */
    protected abstract String exportEmpty();

    /**
     * Записи для списка удаления.
     */
    protected abstract List<E> listForDelete(Integer userId, Integer profileId);

    /**
     * Текст кнопки удаления записи.
     */
    protected abstract String deleteLabel(E record);

    /**
     * id записи.
     */
    protected abstract Integer idOf(E record);

    /**
     * Удалить запись по id.
     */
    protected abstract void delete(Integer userId, Integer profileId, Integer id);

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(openAction(), exportAction(), deleteAction(), deleteSelectedAction());
    }

    @Override
    public final Object handleCallback(Update update, User user, Object marker) {
        CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());
        reply.answerCallbackQuery(update.getCallbackQuery().getId());

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            return null;
        }

        try {
            if (action == openAction()) {
                showSection(user, profile);
            } else if (action == exportAction()) {
                renderExport(user, profile);
            } else if (action == deleteAction()) {
                renderDeleteList(user, profile);
            } else if (action == deleteSelectedAction()) {
                handleDeleteSelected(update, user, profile);
            }
        } catch (RecordOperationException e) {
            handleSectionError(user, e);
        }

        return null;
    }

    private void handleSectionError(User user, RecordOperationException e) {
        if (e.getError() == RecordOperationException.Error.PROFILE_ACCESS_DENIED) {
            reply.send(user, e.getMessage());
            profileSelectionScreen.render(user);
        } else {
            sectionScreen.sendTextWithBack(user, HtmlUtils.escape(e.getMessage()), openAction());
        }
    }

    /**
     * Открыть меню раздела для активного профиля.
     */
    public void showSection(User user) {
        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            return;
        }
        try {
            showSection(user, profile);
        } catch (RecordOperationException e) {
            handleSectionError(user, e);
        }
    }

    private void showSection(User user, Profile profile) {
        sectionScreen.renderSection(user,
                sectionTitle(), profile.getName(),
                sectionPreview(user, profile),
                addLabel(), addAction(), exportAction(), deleteAction());
    }

    private void renderExport(User user, Profile profile) {
        List<E> records = listForExport(user.getId(), profile.getId());
        if (records.isEmpty()) {
            sectionScreen.sendTextWithBack(user, exportEmpty(), openAction());
            return;
        }
        String head = BotTexts.EXPORT_HEAD.formatted(HtmlUtils.bold(profile.getName()));
        String body = records.stream()
                .map(record -> HtmlUtils.escape(exportLine(record)))
                .collect(Collectors.joining("\n"));
        sectionScreen.sendTextWithBack(user, head + "\n\n" + body, openAction());
    }

    private void renderDeleteList(User user, Profile profile) {
        List<E> records = listForDelete(user.getId(), profile.getId());
        List<DeletionData> deletions = records.stream()
                .map(record -> new DeletionData(idOf(record), deleteLabel(record)))
                .toList();
        String head = deletions.isEmpty() ? BotTexts.DELETE_RECORDS_EMPTY : BotTexts.DELETE_LIST_HEAD;
        sectionScreen.renderDeleteList(user, head, deletions, deleteSelectedAction(), openAction());
    }

    private void handleDeleteSelected(Update update, User user, Profile profile) {
        String raw = CallbackAction.payloadOf(update.getCallbackQuery().getData());
        Integer id = Numbers.parseInt(raw);
        if (id == null) {
            return;
        }
        delete(user.getId(), profile.getId(), id);
        reply.send(user, BotTexts.RECORD_DELETED);
        renderDeleteList(user, profile);
    }
}
