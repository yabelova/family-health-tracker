package com.yabelova.healthtracker.wizard;

import com.yabelova.healthtracker.util.Dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
                case Class<?> c when c == LocalDateTime.class -> Dates.parseLocalDateTime(trimmed);
                case Class<?> c when c == LocalDate.class -> Dates.parseLocalDate(trimmed);
                case Class<?> c when c == LocalTime.class -> LocalTime.parse(trimmed, Dates.TIME);
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
     * Форматирует сохраненное значение для показа юзеру в том виде, в котором он вводил
     * (даты/время — в {@code dd.MM.yyyy}, ...). Boolean отображается как «Да»/«Нет».
     *
     * @return {@code "—"} для null
     */
    public static String format(Object value) {
        if (value == null) {
            return "—";
        }
        return switch (value) {
            case LocalDateTime dt -> dt.format(Dates.DATE_TIME);
            case LocalDate d -> d.format(Dates.DATE);
            case LocalTime t -> t.format(Dates.TIME);
            case Boolean b -> b ? "Да" : "Нет";
            default -> value.toString();
        };
    }

    /**
     * Подсказка о формате ввода для типа (показывается юзеру вместе с шагом).
     */
    public static String formatHint(Class<?> type) {
        return switch (type) {
            case Class<?> c when c == LocalDateTime.class -> """
                    Воспользуйтесь кнопками или введите вручную:
                    • ЧЧ:ММ — время сегодня (например, 14:30)
                    • ДД.ММ ЧЧ:ММ — дата в этом году и время (например, 05.09 21:00)
                    • ДД.ММ.ГГГГ ЧЧ:ММ — дата с годом и время (например, 30.12.2025 21:00)
                    • ДД.ММ / ДД.ММ.ГГГГ — дата (без времени) на начало суток (например, 30.12 или 14.03.2026)""";
            case Class<?> c when c == LocalDate.class -> """
                    Воспользуйтесь кнопками или введите вручную:
                    • ДД.ММ.ГГГГ — полная дата (например, 31.08.2026)
                    • ДД.ММ — дата в этом году (например, 31.08)""";
            case Class<?> c when c == LocalTime.class -> """
                    Воспользуйтесь кнопками или введите вручную:
                    • ЧЧ:ММ — время (например, 14:30)""";
            case Class<?> c when c == Boolean.class || c == boolean.class -> "Используйте кнопки «Да» / «Нет»";
            default -> "Введите значение";
        };
    }
}
