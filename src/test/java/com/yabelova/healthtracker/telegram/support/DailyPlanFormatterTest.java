package com.yabelova.healthtracker.telegram.support;

import com.yabelova.healthtracker.service.DailyPlanBuilder.CourseLine;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlan;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlanProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты форматирования текстового плана и экранирования ввода.
 */
class DailyPlanFormatterTest {

    private final DailyPlanFormatter formatter = new DailyPlanFormatter();

    @Test
    void formatsProfilesAndCourses() {
        DailyPlan plan = new DailyPlan(List.of(
                new DailyPlanProfile("Профиль с дозами", List.of(new CourseLine("Антибиотик", 2))),
                new DailyPlanProfile("Профиль без доз", List.of(new CourseLine("Полоскание", null)))));

        String text = formatter.format(plan);

        assertThat(text).isEqualTo("""
                На сегодня запланирован приём:
                
                • <b>Профиль с дозами</b>:
                  Антибиотик — 2 доз
                • <b>Профиль без доз</b>:
                  Полоскание""");
    }

    @Test
    void formatsProfileWithFewCourses() {
        DailyPlan plan = new DailyPlan(List.of(
                new DailyPlanProfile("Я", List.of(
                        new CourseLine("Антибиотик", 2),
                        new CourseLine("Витамины", 1),
                        new CourseLine("Чай с лимоном", null)))));

        String text = formatter.format(plan);

        assertThat(text).isEqualTo("""
                На сегодня запланирован приём:
                
                • <b>Я</b>:
                  Антибиотик — 2 доз
                  Витамины — 1 доз
                  Чай с лимоном""");
    }

    @Test
    void escapesUserInput() {
        DailyPlan plan = new DailyPlan(List.of(
                new DailyPlanProfile("Про<филь>", List.of(new CourseLine("Анти&биотик", 3)))));

        String text = formatter.format(plan);

        assertThat(text).contains("• <b>Про&lt;филь&gt;</b>:\n  Анти&amp;биотик — 3 доз");
    }
}
