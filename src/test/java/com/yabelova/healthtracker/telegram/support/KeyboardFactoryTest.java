package com.yabelova.healthtracker.telegram.support;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест быстрых кнопок шагов анкеты по типу поля.
 */
class KeyboardFactoryTest {

    private final KeyboardFactory keyboard = new KeyboardFactory();

    @Test
    void dateStepOffersTodayYesterdayTomorrow() {
        List<String> data = callbackData(keyboard.formStepKeyboard(LocalDate.class, false));
        assertThat(data).contains(
                "wizard.quick.set:today",
                "wizard.quick.set:yesterday",
                "wizard.quick.set:tomorrow");
    }

    @Test
    void dateTimeStepOffersNow() {
        List<String> data = callbackData(keyboard.formStepKeyboard(LocalDateTime.class, false));
        assertThat(data).contains("wizard.quick.set:now");
    }

    @Test
    void timeStepOffersNow() {
        List<String> data = callbackData(keyboard.formStepKeyboard(LocalTime.class, false));
        assertThat(data).contains("wizard.quick.set:now");
    }

    @Test
    void booleanStepOffersYesNoAndOptionalAddsSkip() {
        List<String> data = callbackData(keyboard.formStepKeyboard(Boolean.class, true));
        assertThat(data).contains("wizard.bool.yes", "wizard.bool.no", "wizard.skip");
    }

    @Test
    void textStepHasNoButtons() {
        assertThat(keyboard.formStepKeyboard(String.class, false)).isNull();
    }

    private List<String> callbackData(InlineKeyboardMarkup markup) {
        List<String> data = new ArrayList<>();
        if (markup == null) {
            return data;
        }
        for (var row : markup.getKeyboard()) {
            for (var button : row) {
                data.add(button.getCallbackData());
            }
        }
        return data;
    }
}
