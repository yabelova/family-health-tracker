package com.yabelova.healthtracker.telegram.support;

/**
 * Слова-подтверждения, которые пользователь вводит текстом
 * (сравниваются через equalsIgnoreCase, кнопкой не заменяются).
 */
public final class ConfirmationWords {

    public static final String DELETE = "удалить";
    public static final String TRANSFER = "передать";

    private ConfirmationWords() {
    }
}
