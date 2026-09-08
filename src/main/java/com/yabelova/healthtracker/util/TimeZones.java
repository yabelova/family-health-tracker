package com.yabelova.healthtracker.util;

import java.time.ZoneId;

/**
 * Часовой пояс приложения. Записи ведутся в одном поясе пользователей (МСК).
 * TODO после MVP: настройка таймзоны пользователем.
 */
public final class TimeZones {

    public static final ZoneId DEFAULT = ZoneId.of("Europe/Moscow");

    private TimeZones() {
    }
}
