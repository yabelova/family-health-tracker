package com.yabelova.healthtracker.telegram.support;

import java.util.EnumSet;
import java.util.Set;

public enum CallbackAction {

    // Профиль — вход (выбор / создание / добавление по коду)
    PROFILE_ACTIVATE("profile.activate"),
    PROFILE_CREATE("profile.create"),
    PROFILE_ADD_BY_CODE("profile.add"),

    // Навигация (главное меню разделов)
    MAIN_MENU_ACTION("menu.main"),

    // Курсы приема лекарств (раздел)
    MEDICATION_COURSE("medication.course"),
    MEDICATION_ADD("medication.add"),
    MEDICATION_EXPORT("medication.export"),
    MEDICATION_DELETE("medication.delete"),
    MEDICATION_DELETE_SELECTED("medication.delete.selected"),

    // Симптомы (раздел)
    SYMPTOM_LOG("symptom.log"),
    SYMPTOM_ADD("symptom.add"),
    SYMPTOM_EXPORT("symptom.export"),
    SYMPTOM_DELETE("symptom.delete"),
    SYMPTOM_DELETE_SELECTED("symptom.delete.selected"),

    // Приемы лекарств (раздел)
    INTAKE_LOG("intake.log"),
    INTAKE_ADD("intake.add"),
    INTAKE_EXPORT("intake.export"),
    INTAKE_DELETE("intake.delete"),
    INTAKE_DELETE_SELECTED("intake.delete.selected"),

    // Приемы лекарств (флоу отметки)
    INTAKE_COURSE("intake.course"),
    INTAKE_DOSES("intake.doses"),
    INTAKE_TAKEN_AT("intake.taken-at"),

    // Профиль — управление (меню владельца)
    PROFILE_MANAGE("profile.manage"),
    PROFILE_SHARE("profile.share"),
    PROFILE_RENAME("profile.rename"),
    PROFILE_REVOKE("profile.revoke"),
    PROFILE_TRANSFER("profile.transfer"),
    PROFILE_TRANSFER_OWNERSHIP("profile.transfer.ownership"),
    PROFILE_DELETE("profile.delete"),

    // Уведомления
    NOTIFICATION_EDIT("notification.edit"),
    NOTIFICATION_DISABLE("notification.disable"),

    // Приватность (передача/отзыв данных при удалении аккаунта)
    PRIVACY_TRANSFER("privacy.transfer"),
    PRIVACY_REVOKE("privacy.revoke"),

    // Визард — шаги анкеты и подтверждение
    WIZARD_BOOL_YES("wizard.bool.yes"),
    WIZARD_BOOL_NO("wizard.bool.no"),
    WIZARD_SKIP("wizard.skip"),
    WIZARD_QUICK_SET("wizard.quick.set"),
    WIZARD_CONFIRM("wizard.confirm"),
    WIZARD_RETRY("wizard.retry"),
    WIZARD_CANCEL("wizard.cancel");

    private final String prefix;

    private static final Set<CallbackAction> WIZARD_ACTIONS = EnumSet.of(
            WIZARD_CONFIRM, WIZARD_RETRY, WIZARD_CANCEL, WIZARD_SKIP, WIZARD_QUICK_SET,
            WIZARD_BOOL_YES, WIZARD_BOOL_NO
    );

    CallbackAction(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    /**
     * true для универсальных кнопок визарда (confirm/retry/cancel/skip/quick/bool_yes/bool_no),
     * которые маршрутизируются в команду, ожидающую ввод.
     */
    public boolean isWizardAction() {
        return WIZARD_ACTIONS.contains(this);
    }

    /**
     * Распознает действие по строке callback кнопки (например "profile.activate:1"),
     * отбрасывая payload (часть после двоеточия). Возвращает null, если префикс не распознан.
     */
    public static CallbackAction fromData(String data) {
        if (data == null) {
            return null;
        }
        String action = data.contains(":") ? data.substring(0, data.indexOf(':')) : data;
        for (CallbackAction value : values()) {
            if (value.prefix.equals(action)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Извлекает payload из строки callback кнопки (часть после двоеточия)
     */
    public static String payloadOf(String data) {
        return data.substring(data.indexOf(':') + 1);
    }
}
