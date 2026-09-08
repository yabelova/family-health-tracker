package com.yabelova.healthtracker.telegram.support;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.repository.ProfileRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {

    /**
     * Постоянная reply-панель: рисуется один раз на /start и живет сама.
     * Кнопки всегда одни и те же, без условий.
     */
    public ReplyKeyboardMarkup navigation() {
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(BotTexts.REPLY_BTN_PROFILE_MENU));
        row1.add(new KeyboardButton(BotTexts.REPLY_BTN_SELECT_PROFILE));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(BotTexts.REPLY_BTN_NOTIFICATIONS));
        row2.add(new KeyboardButton(BotTexts.REPLY_BTN_HELP));
        rows.add(row2);

        return ReplyKeyboardMarkup.builder()
                .keyboard(rows)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Контекстное меню профиля (действия по выбранному профилю)
     * Владельцу добавляется переход к экрану управления
     */
    public InlineKeyboardMarkup profileMenu(boolean isOwner) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_TAKE, CallbackAction.INTAKE_LOG.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_COURSE, CallbackAction.MEDICATION_COURSE.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_SYMPTOM, CallbackAction.SYMPTOM_LOG.prefix())));
        if (isOwner) {
            rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_MANAGE, CallbackAction.PROFILE_MANAGE.prefix())));
        }
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_SECTION_BACK, CallbackAction.MAIN_MENU_ACTION.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Меню раздела записей: Добавить / Выгрузить / Удалить.
     * Подпись кнопки добавления передается (для приемов — «Отметить прием»).
     */
    public InlineKeyboardMarkup sectionMenu(String addLabel,
                                            CallbackAction add,
                                            CallbackAction export,
                                            CallbackAction delete) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(inlineButton(addLabel, add.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_SECTION_EXPORT, export.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_SECTION_DELETE, delete.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_SECTION_BACK, CallbackAction.MAIN_MENU_ACTION.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Список записей для удаления: кнопка на каждую (callback deleteSelected:id)
     * + внизу кнопка «Назад» в раздел.
     */
    public InlineKeyboardMarkup recordDeleteList(List<RecordDeleteOption> options,
                                                 CallbackAction deleteSelected,
                                                 CallbackAction back) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (RecordDeleteOption option : options) {
            rows.add(List.of(inlineButton(option.label(),
                    deleteSelected.prefix() + ":" + option.id())));
        }
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_SECTION_BACK, back.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Клавиатура выгрузки: кнопка «Назад» в раздел.
     */
    public InlineKeyboardMarkup backToSection(CallbackAction back) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(inlineButton(BotTexts.INLINE_BTN_SECTION_BACK, back.prefix()))))
                .build();
    }

    /**
     * Экран «Управление профилем»: только для владельца
     * Навигация — reply-кнопкой меню профиля
     */
    public InlineKeyboardMarkup manageMenu() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_SHARE, CallbackAction.PROFILE_SHARE.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_RENAME, CallbackAction.PROFILE_RENAME.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_REVOKE, CallbackAction.PROFILE_REVOKE.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_TRANSFER, CallbackAction.PROFILE_TRANSFER.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_DELETE, CallbackAction.PROFILE_DELETE.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Меню выбора участника для передачи прав: кнопка на каждого участника
     * (callbackData action:profileId:userId) + опционально «Отозвать доступ»
     */
    public InlineKeyboardMarkup transferChoices(List<ProfileRepository.ProfileParticipant> participants,
                                                CallbackAction action,
                                                Integer profileId,
                                                boolean includeRevoke) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ProfileRepository.ProfileParticipant participant : participants) {
            rows.add(List.of(inlineButton(
                    BotTexts.INLINE_BTN_TRANSFER_TO + ParticipantName.of(participant),
                    action.prefix() + ":" + profileId + ":" + participant.userId())));
        }

        if (includeRevoke) {
            rows.add(List.of(inlineButton(
                    BotTexts.INLINE_BTN_MENU_REVOKE,
                    CallbackAction.PRIVACY_REVOKE.prefix() + ":" + profileId)));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Меню выбора профиля: список профилей + создание нового + добавление по коду
     */
    public InlineKeyboardMarkup profileSelection(List<Profile> profiles, Integer activeProfileId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Profile profile : profiles) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            boolean isActive = profile.getId().equals(activeProfileId);
            btn.setText((isActive ? BotTexts.PROFILE_BTN_ACTIVE_PREFIX : "")
                    + BotTexts.PROFILE_BTN_PREFIX + profile.getName());
            btn.setCallbackData(CallbackAction.PROFILE_ACTIVATE.prefix() + ":" + profile.getId());
            rows.add(List.of(btn));
        }

        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_CREATE_PROFILE, CallbackAction.PROFILE_CREATE.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_ADD_BY_CODE, CallbackAction.PROFILE_ADD_BY_CODE.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Меню уведомлений: ввести/изменить время + выключить
     */
    public InlineKeyboardMarkup notifications(boolean hasTime) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String editLabel = hasTime
                ? BotTexts.INLINE_BTN_NOTIFICATION_EDIT
                : BotTexts.INLINE_BTN_NOTIFICATION_SET;
        rows.add(List.of(inlineButton(editLabel, CallbackAction.NOTIFICATION_EDIT.prefix())));
        if (hasTime) {
            rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_NOTIFICATION_DISABLE,
                    CallbackAction.NOTIFICATION_DISABLE.prefix())));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Клавиатура шага анкеты:
     * - для типов дат и времени — быстрые кнопки (Сегодня/Вчера/Завтра, Сейчас)
     * - для булева — «Да/Нет»
     * - для optional-поля дополнительно кнопка «Пропустить».
     * Когда ничего из этого не подходит — вернуть null (ожидаем свободный текст без кнопок).
     */
    public InlineKeyboardMarkup formStepKeyboard(Class<?> type, boolean isOptional) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (type == LocalDate.class) {
            rows.add(List.of(
                    inlineButton(BotTexts.INLINE_BTN_WIZARD_TODAY, CallbackAction.WIZARD_QUICK_SET.prefix() + ":today"),
                    inlineButton(BotTexts.INLINE_BTN_WIZARD_YESTERDAY, CallbackAction.WIZARD_QUICK_SET.prefix() + ":yesterday"),
                    inlineButton(BotTexts.INLINE_BTN_WIZARD_TOMORROW, CallbackAction.WIZARD_QUICK_SET.prefix() + ":tomorrow")));
        } else if (type == LocalDateTime.class || type == LocalTime.class) {
            rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_WIZARD_NOW, CallbackAction.WIZARD_QUICK_SET.prefix() + ":now")));
        } else if (isBoolean(type)) {
            rows.add(List.of(
                    inlineButton(BotTexts.INLINE_BTN_BOOL_YES, CallbackAction.WIZARD_BOOL_YES.prefix()),
                    inlineButton(BotTexts.INLINE_BTN_BOOL_NO, CallbackAction.WIZARD_BOOL_NO.prefix())));
        }

        if (isOptional) {
            rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_WIZARD_SKIP, CallbackAction.WIZARD_SKIP.prefix())));
        }

        if (rows.isEmpty()) {
            return null;
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Клавиатура подтверждения анкеты: подтвердить / начать заново / отмена.
     */
    public InlineKeyboardMarkup formConfirmKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_WIZARD_CONFIRM, CallbackAction.WIZARD_CONFIRM.prefix())));
        rows.add(List.of(
                inlineButton(BotTexts.INLINE_BTN_WIZARD_RETRY, CallbackAction.WIZARD_RETRY.prefix()),
                inlineButton(BotTexts.INLINE_BTN_WIZARD_CANCEL, CallbackAction.WIZARD_CANCEL.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Флоу отметки приема — шаг «препарат»: кнопка на каждый активный курс
     * (callback {@code intake.course:id}). Если курсов нет — возвращает null
     * (ожидаем ручной ввод названия).
     */
    public InlineKeyboardMarkup intakeMedication(List<MedicationCourse> courses) {
        if (courses.isEmpty()) {
            return null;
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (MedicationCourse course : courses) {
            rows.add(List.of(inlineButton(
                    courseMedicationLabel(course),
                    CallbackAction.INTAKE_COURSE.prefix() + ":" + course.getId())));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Флоу отметки приема — шаг «дозы»: быстрые кнопки 1 / 2 и «Пропустить».
     */
    public InlineKeyboardMarkup intakeDoses() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                inlineButton("1", CallbackAction.INTAKE_DOSES.prefix() + ":1"),
                inlineButton("2", CallbackAction.INTAKE_DOSES.prefix() + ":2")));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_WIZARD_SKIP, CallbackAction.WIZARD_SKIP.prefix())));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Флоу отметки приема — шаг «время»: кнопка «Сейчас».
     */
    public InlineKeyboardMarkup intakeTime() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(inlineButton(
                        BotTexts.INTAKE_BTN_NOW, CallbackAction.INTAKE_TAKEN_AT.prefix()))))
                .build();
    }

    private String courseMedicationLabel(MedicationCourse course) {
        return course.getProperties().getMedication();
    }

    private boolean isBoolean(Class<?> type) {
        return type == Boolean.class || type == boolean.class;
    }

    private InlineKeyboardButton inlineButton(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }
}
