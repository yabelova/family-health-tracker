package com.yabelova.healthtracker.telegram.support;

/**
 * Токены slash-команд. Используются как ключи маршрутизации в textKeys().
 */
public enum Commands {

    START("/start"),
    HELP("/help"),
    DELETE_ALL_DATA("/delete_all_data");

    private final String token;

    Commands(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }
}
