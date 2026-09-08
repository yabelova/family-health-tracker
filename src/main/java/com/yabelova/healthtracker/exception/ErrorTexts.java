package com.yabelova.healthtracker.exception;

public final class ErrorTexts {

    public static final String INVITE_NOT_FOUND = "⚠️ Код недействителен. Проверьте ещё раз";
    public static final String INVITE_USED = "⚠️ Код уже использован";
    public static final String INVITE_EXPIRED = "⚠️ Код истёк. Попросите новый";
    public static final String OWN_PROFILE = "⚠️ Это ваш собственный профиль";
    public static final String ALREADY_LINKED = "⚠️ Профиль уже в вашем списке";
    public static final String NOT_OWNER = "⚠️ Только владелец профиля может это сделать";
    public static final String PROFILE_HAS_PARTICIPANTS =
            "⚠️ В профиле есть другие участники. Сначала отзовите им доступ, затем удаляйте";
    public static final String NOT_FOUND_PARTICIPANT = "⚠️ Пользователь больше не участник этого профиля";
    public static final String RECORD_NOT_FOUND = "⚠️ Запись не найдена";
    public static final String RECORD_NOT_LINKED = "⚠️ Запись не принадлежит этому профилю";
    public static final String RECORD_PROFILE_ACCESS_DENIED = "⚠️ К профилю нет доступа";

    private ErrorTexts() {
    }
}
