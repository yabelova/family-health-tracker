package com.yabelova.healthtracker.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Парсеры гибких форматов даты/времени.
 */
class DatesTest {

    // ===== parseLocalDateTime =====

    @Test
    void parsesFullDateTime() {
        assertThat(Dates.parseLocalDateTime("05.09.2025 21:00"))
                .isEqualTo(LocalDateTime.of(2025, 9, 5, 21, 0));
    }

    @Test
    void shortDateTimeDefaultsToCurrentYear() {
        LocalDateTime parsed = Dates.parseLocalDateTime("05.09 21:00");
        assertThat(parsed).isEqualTo(LocalDateTime.of(LocalDate.now(TimeZones.DEFAULT).getYear(), 9, 5, 21, 0));
    }

    @Test
    void fullDateParsesToStartOfDay() {
        assertThat(Dates.parseLocalDateTime("05.09.2025"))
                .isEqualTo(LocalDateTime.of(2025, 9, 5, 0, 0));
    }

    @Test
    void shortDateParsesToStartOfDayOfCurrentYear() {
        LocalDateTime parsed = Dates.parseLocalDateTime("05.09");
        assertThat(parsed).isEqualTo(LocalDateTime.of(LocalDate.now(TimeZones.DEFAULT).getYear(), 9, 5, 0, 0));
    }

    @Test
    void timeOnlyParsesToToday() {
        LocalDate today = LocalDate.now(TimeZones.DEFAULT);
        LocalDateTime parsed = Dates.parseLocalDateTime("21:00");
        assertThat(parsed.toLocalDate()).isEqualTo(today);
        assertThat(parsed.toLocalTime()).isEqualTo(LocalTime.of(21, 0));
    }

    @Test
    void rejectsGarbageAndBlank() {
        assertThat(Dates.parseLocalDateTime(null)).isNull();
        assertThat(Dates.parseLocalDateTime("")).isNull();
        assertThat(Dates.parseLocalDateTime("   ")).isNull();
        assertThat(Dates.parseLocalDateTime("abc")).isNull();
        assertThat(Dates.parseLocalDateTime("14.30")).isNull();
        assertThat(Dates.parseLocalDateTime("31.13.2026 10:00")).isNull();
    }

    // ===== parseLocalDate =====

    @Test
    void parsesFullDate() {
        assertThat(Dates.parseLocalDate("05.09.2025")).isEqualTo(LocalDate.of(2025, 9, 5));
    }

    @Test
    void shortDateDefaultsToCurrentYear() {
        assertThat(Dates.parseLocalDate("05.09"))
                .isEqualTo(LocalDate.of(LocalDate.now(TimeZones.DEFAULT).getYear(), 9, 5));
    }

    @Test
    void rejectsInvalidDate() {
        assertThat(Dates.parseLocalDate(null)).isNull();
        assertThat(Dates.parseLocalDate("")).isNull();
        assertThat(Dates.parseLocalDate("31.13")).isNull();
        assertThat(Dates.parseLocalDate("abc")).isNull();
    }

    // ===== parseLocalTime =====

    @Test
    void parsesTime() {
        assertThat(Dates.parseLocalTime("14:30")).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    void leadingZeroHourIsOptional() {
        assertThat(Dates.parseLocalTime("08:30")).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void rejectsInvalidTime() {
        assertThat(Dates.parseLocalTime(null)).isNull();
        assertThat(Dates.parseLocalTime("")).isNull();
        assertThat(Dates.parseLocalTime("abc")).isNull();
        assertThat(Dates.parseLocalTime("25:00")).isNull();
        assertThat(Dates.parseLocalTime("14:60")).isNull();
    }
}
