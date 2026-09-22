package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import com.yabelova.healthtracker.repository.MedicationCourseRepository.MedicationCoursePlanRow;
import com.yabelova.healthtracker.service.DailyPlanBuilder.CourseLine;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlan;
import com.yabelova.healthtracker.service.DailyPlanBuilder.DailyPlanProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты сборки ежедневного плана приема.
 */
class DailyPlanBuilderTest {

    private final MedicationCourseRepository repository = mock(MedicationCourseRepository.class);
    private final DailyPlanBuilder builder = new DailyPlanBuilder(repository);

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 18);

    @Test
    void buildReturnsPlanForSingleUser() {
        User user = User.builder().id(1).build();
        when(repository.findMedicationCoursePlanRowsByUserIds(List.of(1), TODAY))
                .thenReturn(List.of(
                        new MedicationCoursePlanRow(1, 1, "Я", "Антибиотик", 2),
                        new MedicationCoursePlanRow(1, 1, "Я", "Витамины", 1),
                        new MedicationCoursePlanRow(1, 2, "Бегемот", "Таблетки", null)));

        DailyPlan plan = builder.build(user, TODAY);

        assertThat(plan.profiles()).containsExactly(
                new DailyPlanProfile("Я", List.of(
                        new CourseLine("Антибиотик", 2),
                        new CourseLine("Витамины", 1))),
                new DailyPlanProfile("Бегемот", List.of(
                        new CourseLine("Таблетки", null))));
    }

    @Test
    void buildAllSeparatesPersonalAndSharedProfiles() {
        when(repository.findMedicationCoursePlanRowsByUserIds(List.of(1, 2), TODAY))
                .thenReturn(List.of(
                        new MedicationCoursePlanRow(1, 1, "Я", "Антибиотик", 2),
                        new MedicationCoursePlanRow(1, 2, "Бегемот", "Витамины", 1),
                        new MedicationCoursePlanRow(1, 2, "Бегемот", "Капли", 3),
                        new MedicationCoursePlanRow(2, 2, "Бегемот", "Витамины", 1),
                        new MedicationCoursePlanRow(2, 2, "Бегемот", "Капли", 3)));

        Map<Integer, DailyPlan> plans = builder.buildAll(List.of(1, 2), TODAY);

        assertThat(plans).containsOnlyKeys(1, 2);
        assertThat(plans.get(1).profiles()).containsExactly(
                new DailyPlanProfile("Я", List.of(new CourseLine("Антибиотик", 2))),
                new DailyPlanProfile("Бегемот", List.of(new CourseLine("Витамины", 1), new CourseLine("Капли", 3))));
        assertThat(plans.get(2).profiles()).containsExactly(
                new DailyPlanProfile("Бегемот", List.of(new CourseLine("Витамины", 1), new CourseLine("Капли", 3))));
    }

    @Test
    void buildWithNoRowsReturnsEmptyPlan() {
        User user = User.builder().id(1).build();
        when(repository.findMedicationCoursePlanRowsByUserIds(List.of(1), TODAY)).thenReturn(List.of());

        assertThat(builder.build(user, TODAY).profiles()).isEmpty();
    }

    @Test
    void buildAllWithEmptyUserIdsSkipsRepository() {
        assertThat(builder.buildAll(List.of(), TODAY)).isEmpty();
        verify(repository, never()).findMedicationCoursePlanRowsByUserIds(any(), any());
    }
}
