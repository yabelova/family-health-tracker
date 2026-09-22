package com.yabelova.healthtracker.util;

/**
 * Парсинг целых и дробных чисел из ввода юзера (id из callback-данных, количества доз и т.п.).
 * Возвращает {@code null} вместо выброса исключения, если строка не является числом.
 */
public final class Numbers {

    private Numbers() {
    }

    public static Integer parseInt(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double parseDouble(String raw) {
        try {
            return Double.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
