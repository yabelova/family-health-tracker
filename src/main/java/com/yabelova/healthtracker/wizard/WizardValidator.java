package com.yabelova.healthtracker.wizard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Валидация и конвертация свободного текстового ввода юзера на шаге анкеты в значение нужного типа.
 * Поддерживаемые типы соответствуют полям data-классов (String, LocalDateTime, ...).
 */
public final class WizardValidator {

    private static final Set<Class<?>> SUPPORTED_TYPES = Set.of(
            String.class,
            LocalDateTime.class,
            LocalDate.class,
            LocalTime.class,
            Integer.class, int.class,
            Double.class, double.class,
            Boolean.class, boolean.class
    );

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

    private WizardValidator() {
    }

    /**
     * Возвращает true, если тип поля поддерживается анкетой (можно конвертировать в {@link #convertIfValid})
     */
    public static boolean supports(Class<?> type) {
        return SUPPORTED_TYPES.contains(type);
    }

    /**
     * Конвертирует ввод юзера в значение типа {@code type}.
     * Если ввод пуст или некорректен для типа, возвращает {@code null}
     */
    public static Object convertIfValid(String input, Class<?> type) {
        if (input == null || input.isBlank() || type == null) {
            return null;
        }
        String trimmed = input.trim();
        try {
            return switch (type) {
                case Class<?> c when c == String.class -> trimmed;
                case Class<?> c when c == LocalDateTime.class -> LocalDateTime.parse(trimmed, DATETIME);
                case Class<?> c when c == LocalDate.class -> LocalDate.parse(trimmed, DATE);
                case Class<?> c when c == LocalTime.class -> LocalTime.parse(trimmed, TIME);
                case Class<?> c when c == Integer.class || c == int.class -> Integer.parseInt(trimmed);
                case Class<?> c when c == Double.class || c == double.class -> Double.parseDouble(trimmed);
                case Class<?> c when c == Boolean.class || c == boolean.class -> null;
                default -> null;
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Форматирует сохранённое значение для показа юзеру в том виде, в котором он вводил
     * (даты/время — в {@code dd.MM.yyyy}, ...). Boolean отображается как «Да»/«Нет».
     *
     * @return {@code "—"} для null
     */
    public static String format(Object value) {
        if (value == null) {
            return "—";
        }
        return switch (value) {
            case LocalDateTime dt -> dt.format(DATETIME);
            case LocalDate d -> d.format(DATE);
            case LocalTime t -> t.format(TIME);
            case Boolean b -> b ? "Да" : "Нет";
            default -> value.toString();
        };
    }

    /**
     * Подсказка о формате ввода для типа (показывается юзеру вместе с шагом).
     */
    public static String formatHint(Class<?> type) {
        return switch (type) {
            case Class<?> c when c == LocalDateTime.class -> "Формат: дд.мм.гггг чч:мм (например, 31.08.2026 14:30)";
            case Class<?> c when c == LocalDate.class -> "Формат: дд.мм.гггг (например, 31.08.2026)";
            case Class<?> c when c == LocalTime.class -> "Формат: ч:мм (например, 8:30 или 12:45)";
            case Class<?> c when c == Boolean.class || c == boolean.class -> "Используйте кнопки «Да» / «Нет»";
            default -> "Введите значение";
        };
    }
}
