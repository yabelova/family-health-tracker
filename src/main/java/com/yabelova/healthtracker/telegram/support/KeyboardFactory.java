package com.yabelova.healthtracker.telegram.support;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.telegram.CallbackAction;
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

    // ===== REPLY-КНОПКИ (нижняя панель ввода) =====

    // Навигация (всегда доступные)
    public static final String REPLY_BTN_NOTIFICATIONS = "🔔 Уведомления";
    public static final String REPLY_BTN_SELECT_PROFILE = "👤 Выбор профиля";

    // Навигация (только при выбранном профиле)
    public static final String REPLY_BTN_PROFILE_MENU = "📋 Меню профиля";

    // ===== INLINE-КНОПКИ =====

    // Экран «Выбор профиля»
    public static final String INLINE_BTN_CREATE_PROFILE = "➕ Создать новый профиль";

    // Экран «Уведомления»
    public static final String INLINE_BTN_NOTIFICATION_EDIT = "✏️ Изменить время";
    public static final String INLINE_BTN_NOTIFICATION_SET = "⏰ Ввести время";
    public static final String INLINE_BTN_NOTIFICATION_DISABLE = "🚫 Выключить уведомления";

    // Экран «Меню профиля» (действия)
    public static final String INLINE_BTN_MENU_TAKE = "💊 Принять лекарство";
    public static final String INLINE_BTN_MENU_PLAN = "📋 План лечения";
    public static final String INLINE_BTN_MENU_SYMPTOM = "📝 Записать симптом";

    /**
     * Reply-панель показывается в зависимости от того, выбран ли профиль.
     */
    public ReplyKeyboardMarkup navigation(boolean profileSelected) {
        List<KeyboardRow> rows = new ArrayList<>();

        if (profileSelected) {
            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton(REPLY_BTN_PROFILE_MENU));
            rows.add(row1);
        }

        KeyboardRow navRow = new KeyboardRow();
        navRow.add(new KeyboardButton(REPLY_BTN_NOTIFICATIONS));
        navRow.add(new KeyboardButton(REPLY_BTN_SELECT_PROFILE));
        rows.add(navRow);

        return ReplyKeyboardMarkup.builder()
                .keyboard(rows)
                .resizeKeyboard(true)
                .build();
    }

    /**
     * Контекстное меню профиля (действия по выбранному профилю).
     */
    public InlineKeyboardMarkup profileMenu() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(inlineButton(INLINE_BTN_MENU_TAKE, CallbackAction.MAIN_MENU_ACTION.prefix())));
        rows.add(List.of(inlineButton(INLINE_BTN_MENU_PLAN, CallbackAction.MAIN_MENU_ACTION.prefix())));
        rows.add(List.of(inlineButton(INLINE_BTN_MENU_SYMPTOM, CallbackAction.MAIN_MENU_ACTION.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Меню выбора профиля: список профилей + создание нового.
     */
    public InlineKeyboardMarkup profileSelection(List<Profile> profiles, Integer activeProfileId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Profile profile : profiles) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            boolean isActive = profile.getId().equals(activeProfileId);
            btn.setText((isActive ? "✓ " : "") + "👤 " + profile.getName());
            btn.setCallbackData(CallbackAction.ACTIVATE_PROFILE.prefix() + ":" + profile.getId());
            rows.add(List.of(btn));
        }

        rows.add(List.of(inlineButton(INLINE_BTN_CREATE_PROFILE, CallbackAction.CREATE_PROFILE.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Меню уведомлений: ввести/изменить время + выключить.
     */
    public InlineKeyboardMarkup notifications(boolean hasTime) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String editLabel = hasTime ? INLINE_BTN_NOTIFICATION_EDIT : INLINE_BTN_NOTIFICATION_SET;
        rows.add(List.of(inlineButton(editLabel, CallbackAction.NOTIFICATION_EDIT.prefix())));
        if (hasTime)
            rows.add(List.of(inlineButton(INLINE_BTN_NOTIFICATION_DISABLE, CallbackAction.NOTIFICATION_DISABLE.prefix())));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton inlineButton(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }
}
