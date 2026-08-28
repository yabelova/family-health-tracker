package com.yabelova.healthtracker.exception;

public class ProfileOperationException extends RuntimeException {

    private final Error error;

    public ProfileOperationException(Error error) {
        super(messageOf(error));
        this.error = error;
    }

    public Error getError() {
        return error;
    }

    public enum Error {
        INVITE_NOT_FOUND, INVITE_USED, INVITE_EXPIRED, OWN_PROFILE, ALREADY_LINKED, NOT_OWNER
    }

    private static String messageOf(Error error) {
        return switch (error) {
            case INVITE_NOT_FOUND -> "⚠️ Код недействителен. Проверьте ещё раз";
            case INVITE_USED -> "⚠️ Код уже использован";
            case INVITE_EXPIRED -> "⚠️ Код истёк. Попросите новый";
            case OWN_PROFILE -> "⚠️ Это ваш собственный профиль";
            case ALREADY_LINKED -> "⚠️ Профиль уже в вашем списке";
            case NOT_OWNER -> "⚠️ Только владелец профиля может это сделать";
        };
    }
}