package com.yabelova.healthtracker.telegram.support;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.repository.ProfileRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {

    /**
     * Постоянная reply-панель: рисуется один раз на /start и живёт сама.
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

        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_TAKE, CallbackAction.MAIN_MENU_ACTION.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_PLAN, CallbackAction.MAIN_MENU_ACTION.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_SYMPTOM, CallbackAction.MAIN_MENU_ACTION.prefix())));

        if (isOwner) {
            rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_MENU_MANAGE, CallbackAction.PROFILE_MANAGE.prefix())));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
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
            btn.setCallbackData(CallbackAction.ACTIVATE_PROFILE.prefix() + ":" + profile.getId());
            rows.add(List.of(btn));
        }

        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_CREATE_PROFILE, CallbackAction.CREATE_PROFILE.prefix())));
        rows.add(List.of(inlineButton(BotTexts.INLINE_BTN_ADD_BY_CODE, CallbackAction.PROFILE_ADD.prefix())));

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

    private InlineKeyboardButton inlineButton(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }
}
