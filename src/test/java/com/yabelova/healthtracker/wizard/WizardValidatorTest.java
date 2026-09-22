package com.yabelova.healthtracker.wizard;

import com.yabelova.healthtracker.util.TimeZones;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты конвертации свободного ввода визарда по типам: диспетчеризация, обрезка, null при пустом или некорректном
 * вводе, форматирование сохраненных значений.
 */
class WizardValidatorTest {

    @Test
    void convertIfValidAcceptsFlexibleDateTime() {
        Object parsed = WizardValidator.convertIfValid("05.09 21:00", LocalDateTime.class);
        assertThat(parsed).isEqualTo(LocalDateTime.of(LocalDate.now(TimeZones.DEFAULT).getYear(), 9, 5, 21, 0));
    }

    @Test
    void convertIfValidParsesNumbers() {
        assertThat(WizardValidator.convertIfValid("5", Integer.class)).isEqualTo(5);
        assertThat(WizardValidator.convertIfValid("2.5", Double.class)).isEqualTo(2.5);
    }

    @Test
    void convertIfValidParsesTime() {
        assertThat(WizardValidator.convertIfValid("14:30", LocalTime.class)).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    void convertIfValidTrimsString() {
        assertThat(WizardValidator.convertIfValid("  текст  ", String.class)).isEqualTo("текст");
    }

    @Test
    void convertIfValidRejectsGarbage() {
        assertThat(WizardValidator.convertIfValid("abc", Integer.class)).isNull();
        assertThat(WizardValidator.convertIfValid("abc", LocalTime.class)).isNull();
        assertThat(WizardValidator.convertIfValid("", Integer.class)).isNull();
        assertThat(WizardValidator.convertIfValid(null, LocalDateTime.class)).isNull();
        assertThat(WizardValidator.convertIfValid("5", null)).isNull();
    }

    @Test
    void convertIfValidIgnoresButtonsAndUnknownTypes() {
        assertThat(WizardValidator.convertIfValid("да", Boolean.class)).isNull();
        assertThat(WizardValidator.convertIfValid("5", Long.class)).isNull();
    }

    @Test
    void formatRendersSavedValues() {
        assertThat(WizardValidator.format(LocalDateTime.of(2026, 9, 5, 21, 0))).isEqualTo("05.09.2026 21:00");
        assertThat(WizardValidator.format(LocalDate.of(2026, 9, 5))).isEqualTo("05.09.2026");
        assertThat(WizardValidator.format(LocalTime.of(14, 30))).isEqualTo("14:30");
        assertThat(WizardValidator.format(true)).isEqualTo("Да");
        assertThat(WizardValidator.format(false)).isEqualTo("Нет");
        assertThat(WizardValidator.format(7)).isEqualTo("7");
        assertThat(WizardValidator.format(2.5)).isEqualTo("2.5");
    }

    @Test
    void formatNullIsDash() {
        assertThat(WizardValidator.format(null)).isEqualTo("—");
    }

    @Test
    void formatHintCoversAllSupportedTypes() {
        assertThat(WizardValidator.formatHint(LocalDateTime.class)).contains("ДД.ММ ЧЧ:ММ").contains("ДД.ММ.ГГГГ");
        assertThat(WizardValidator.formatHint(LocalDate.class)).contains("ДД.ММ");
        assertThat(WizardValidator.formatHint(LocalTime.class)).contains("ЧЧ:ММ");
        assertThat(WizardValidator.formatHint(Boolean.class)).contains("кнопки");
        assertThat(WizardValidator.formatHint(Integer.class)).isEqualTo("Введите значение");
    }
}
