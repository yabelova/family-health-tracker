package com.yabelova.healthtracker.telegram;

public enum CallbackAction {

    ACTIVATE_PROFILE("profile.activate"),

    CREATE_PROFILE("profile.create"),

    PROFILE_MANAGE("profile.manage"),

    PROFILE_SHARE("profile.share"),

    PROFILE_ADD("profile.add"),

    PROFILE_REVOKE("profile.revoke"),

    PROFILE_DELETE("profile.delete"),

    PROFILE_RENAME("profile.rename"),

    MAIN_MENU_ACTION("menu.main"),

    NOTIFICATION_EDIT("notification.edit"),

    NOTIFICATION_DISABLE("notification.disable");

    private final String prefix;

    CallbackAction(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    /**
     * Определяет действие по полному callback-данному (например "profile.activate:1")
     * Возвращает null, если префикс не распознан
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
     * Извлекает payload из callback-данного (часть после двоеточия)
     */
    public static String payloadOf(String data) {
        return data.substring(data.indexOf(':') + 1);
    }
}
