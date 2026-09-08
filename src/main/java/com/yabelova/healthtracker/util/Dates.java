package com.yabelova.healthtracker.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * Единые паттерны даты/времени для парсинга ввода и показа юзеру.
 */
public final class Dates {

    /**
     * Полная дата и время: {@code дд.мм.гггг чч:мм}
     */
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Только дата: {@code дд.мм.гггг}
     */
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Время: {@code чч:мм}; парсит также {@code 8:30} (час без ведущего нуля)
     */
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Короткая дата и время для ввода приема: {@code дд.мм чч:мм}; год подставляется текущий
     */
    public static final DateTimeFormatter SHORT_DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("dd.MM H:mm")
            .parseDefaulting(ChronoField.YEAR, LocalDate.now(TimeZones.DEFAULT).getYear())
            .toFormatter();

    /**
     * Короткая дата для ввода записей: {@code дд.мм}; год подставляется текущий
     */
    public static final DateTimeFormatter SHORT_DATE = new DateTimeFormatterBuilder()
            .appendPattern("dd.MM")
            .parseDefaulting(ChronoField.YEAR, LocalDate.now(TimeZones.DEFAULT).getYear())
            .toFormatter();

    private Dates() {
    }

    /**
     * Гибкий разбор даты/времени для ввода записей (приемы, симптомы).
     * Форматы по убыванию конкретности: полные дата+время, без года (год текущий),
     * дата без времени (начало суток), только время (сегодня).
     *
     * @return {@link LocalDateTime} или {@code null}, если строка не разобрана
     */
    public static LocalDateTime parseLocalDateTime(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        try {
            return LocalDateTime.parse(trimmed, DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        // без года — подставляется текущий
        try {
            return LocalDateTime.parse(trimmed, SHORT_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        // полная дата без времени — начало суток
        try {
            return LocalDate.parse(trimmed, DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        // короткая дата без времени — начало суток, год текущий
        try {
            return LocalDate.parse(trimmed, SHORT_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        // только время — сегодня
        try {
            return LocalDateTime.of(LocalDate.now(TimeZones.DEFAULT), LocalTime.parse(trimmed, TIME));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Гибкий разбор даты для ввода записей (дата начала курса).
     * {@code дд.мм.гггг} или {@code дд.мм} (год текущий).
     *
     * @return {@link LocalDate} или {@code null}, если строка не разобрана
     */
    public static LocalDate parseLocalDate(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        try {
            return LocalDate.parse(trimmed, DATE);
        } catch (DateTimeParseException ignored) {
        }
        // без года — подставляется текущий
        try {
            return LocalDate.parse(trimmed, SHORT_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
