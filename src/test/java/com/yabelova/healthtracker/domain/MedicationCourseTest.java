package com.yabelova.healthtracker.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты доменной логики {@link MedicationCourse}: пересечение нижнего порога нехватки {@code dosesPerDay}
 * при добавлении доз, остаток доз и перерасход.
 */
class MedicationCourseTest {

    @Test
    void crossingThresholdReturnsTrue() {
        MedicationCourse course = course(2, 10, 8);

        assertThat(course.isStockThresholdCrossedBy(1)).isTrue();
    }

    @Test
    void noPackageHasNoCrossing() {
        MedicationCourse course = course(2, null, 3);

        assertThat(course.isStockThresholdCrossedBy(1)).isFalse();
    }

    @Test
    void aboveThresholdHasNoCrossing() {
        MedicationCourse course = course(2, 10, 2);

        assertThat(course.isStockThresholdCrossedBy(1)).isFalse();
    }

    @Test
    void alreadyInLowZoneNotCrossedAgain() {
        MedicationCourse course = course(2, 10, 9);

        assertThat(course.isStockThresholdCrossedBy(1)).isFalse();
    }

    @Test
    void dosesPerDayNullUsesOneAsThreshold() {
        MedicationCourse course = course(null, 3, 2);

        assertThat(course.isStockThresholdCrossedBy(1)).isTrue();
    }

    @Test
    void overrunRepeatIntakeStaysSilent() {
        MedicationCourse course = course(2, 10, 11);

        assertThat(course.isStockThresholdCrossedBy(1)).isFalse();
    }

    @Test
    void overrunWhenTakenExceedsPackage() {
        MedicationCourse course = course(2, 10, 11);

        assertThat(course.isOverrun()).isTrue();
    }

    @Test
    void noOverrunAtExactPackage() {
        MedicationCourse course = course(2, 10, 10);

        assertThat(course.isOverrun()).isFalse();
    }

    @Test
    void noPackageHasNoOverrun() {
        MedicationCourse course = course(2, null, 11);

        assertThat(course.isOverrun()).isFalse();
    }

    @Test
    void remainingIsPackageMinusTaken() {
        MedicationCourse course = course(2, 10, 2);

        assertThat(course.remainingDoses()).isEqualTo(8);
    }

    @Test
    void remainingFloorsAtZeroWhenOverdrawn() {
        MedicationCourse course = course(2, 10, 11);

        assertThat(course.remainingDoses()).isZero();
    }

    @Test
    void noPackageReturnsNullRemaining() {
        MedicationCourse course = course(2, null, 3);

        assertThat(course.remainingDoses()).isNull();
    }

    private MedicationCourse course(Integer dosesPerDay, Integer dosesPerPackage, Integer dosesTaken) {
        MedicationCourseProperties properties = new MedicationCourseProperties();
        properties.setMedication("Антибиотик");
        properties.setDosesPerDay(dosesPerDay);
        properties.setDosesPerPackage(dosesPerPackage);
        return MedicationCourse.builder()
                .profileId(1)
                .properties(properties)
                .dosesTaken(dosesTaken)
                .build();
    }
}
