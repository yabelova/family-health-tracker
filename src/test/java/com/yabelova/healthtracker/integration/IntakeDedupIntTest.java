package com.yabelova.healthtracker.integration;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import com.yabelova.healthtracker.service.IntakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты защиты от двойной отметки приема уникальным индексом {@code uq_med_intake_dedup}.
 */
class IntakeDedupIntTest extends AbstractContainerIntTest {

    private static final LocalDateTime TAKEN_AT = LocalDateTime.of(2026, 9, 20, 9, 30);

    @Autowired
    IntakeService intakeService;

    @Test
    void duplicateCourseIntakeIsRejectedOnce() {
        Integer userId = createUser("Полина");
        Integer profileId = createProfileLinked(userId, true);
        MedicationCourse course = createActiveCourse(profileId, userId);
        MedicationIntakeProperties properties = new MedicationIntakeProperties("Амоксициллин", TAKEN_AT, 1);

        intakeService.save(profileId, userId, course.getId(), properties);

        assertThatThrownBy(() -> intakeService.save(profileId, userId, course.getId(), properties))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(intakeCount()).isEqualTo(1);
        assertThat(courseDosesTaken(course.getId())).isEqualTo(1);
    }

    @Test
    void differentDosesStillDuplicateIntake() {
        Integer userId = createUser("Полина");
        Integer profileId = createProfileLinked(userId, true);
        MedicationCourse course = createActiveCourse(profileId, userId);

        intakeService.save(profileId, userId, course.getId(),
                new MedicationIntakeProperties("Амоксициллин", TAKEN_AT, 1));

        assertThatThrownBy(() -> intakeService.save(profileId, userId, course.getId(),
                new MedicationIntakeProperties("Амоксициллин", TAKEN_AT, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(intakeCount()).isEqualTo(1);
        assertThat(courseDosesTaken(course.getId())).isEqualTo(1);
    }

    @Test
    void manualDuplicateRejectedButDifferentProfileAllowed() {
        Integer user1 = createUser("Полина");
        Integer profile1 = createProfileLinked(user1, true);
        Integer user2 = createUser("Инна");
        Integer profile2 = createProfileLinked(user2, true);
        MedicationIntakeProperties properties = new MedicationIntakeProperties("Амоксициллин", TAKEN_AT, null);

        intakeService.save(profile1, user1, null, properties);

        assertThatThrownBy(() -> intakeService.save(profile1, user1, null, properties))
                .isInstanceOf(DataIntegrityViolationException.class);

        intakeService.save(profile2, user2, null, properties);

        assertThat(intakeCount()).isEqualTo(2);
    }

    @Test
    void subtractDosesTakenNeverGoesNegative() {
        Integer userId = createUser("Полина");
        Integer profileId = createProfileLinked(userId, true);
        MedicationCourse course = createActiveCourse(profileId, userId);
        courses.addDosesTaken(course.getId(), 3);

        courses.subtractDosesTaken(course.getId(), 100);

        assertThat(courseDosesTaken(course.getId())).isZero();
    }

    private int intakeCount() {
        return jdbcTemplate.queryForObject("select count(*) from t_medication_intakes", Integer.class);
    }

    private int courseDosesTaken(Integer courseId) {
        return jdbcTemplate.queryForObject(
                "select doses_taken from t_medication_courses where id = ?", Integer.class, courseId);
    }
}
