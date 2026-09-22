package com.yabelova.healthtracker.integration;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import com.yabelova.healthtracker.domain.UserRole;
import com.yabelova.healthtracker.service.IntakeService;
import com.yabelova.healthtracker.service.IntakeService.IntakeSaveResult;
import com.yabelova.healthtracker.service.MedicationCourseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Конкурентные отметки приема и удаление курса под пессимистичным локом: дозы не теряются и операции не падают.
 */
class CourseIntakeLockIntTest extends AbstractContainerIntTest {

    @Autowired
    IntakeService intakeService;

    @Autowired
    MedicationCourseService courseService;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @AfterEach
    void shutdownPool() {
        pool.shutdownNow();
    }

    @Test
    void parallelIntakesOnSameCourseSumDoses() throws Exception {
        SharedFixture fixture = sharedFixture();
        CountDownLatch start = new CountDownLatch(1);

        Future<IntakeSaveResult> morning = pool.submit(() -> {
            start.await();
            return intakeService.save(fixture.profileId(), fixture.memberId(), fixture.course().getId(),
                    new MedicationIntakeProperties("Амоксициллин", LocalDateTime.of(2026, 9, 20, 9, 30), 1));
        });
        Future<IntakeSaveResult> evening = pool.submit(() -> {
            start.await();
            return intakeService.save(fixture.profileId(), fixture.ownerId(), fixture.course().getId(),
                    new MedicationIntakeProperties("Амоксициллин", LocalDateTime.of(2026, 9, 20, 20, 40), 1));
        });

        start.countDown();
        morning.get(30, TimeUnit.SECONDS);
        evening.get(30, TimeUnit.SECONDS);

        assertThat(intakeCount()).isEqualTo(2);
        assertThat(dosesTaken(fixture.course().getId())).isEqualTo(2);
    }

    @Test
    void concurrentIntakeAndCourseDeleteStayConsistent() {
        SharedFixture fixture = sharedFixture();
        CountDownLatch start = new CountDownLatch(1);

        Future<IntakeSaveResult> intakeFuture = pool.submit(() -> {
            start.await();
            return intakeService.save(fixture.profileId(), fixture.memberId(), fixture.course().getId(),
                    new MedicationIntakeProperties("Амоксициллин", LocalDateTime.of(2026, 9, 20, 9, 30), 1));
        });
        Future<?> deleteFuture = pool.submit(() -> {
            start.await();
            courseService.delete(fixture.ownerId(), fixture.profileId(), fixture.course().getId());
            return null;
        });

        start.countDown();

        assertThatCode(() -> intakeFuture.get(30, TimeUnit.SECONDS)).doesNotThrowAnyException();
        assertThatCode(() -> deleteFuture.get(30, TimeUnit.SECONDS)).doesNotThrowAnyException();

        assertThat(courses.existsById(fixture.course().getId())).isFalse();
        assertThat(intakeCount()).isEqualTo(1);
        List<Integer> courseIds = jdbcTemplate.queryForList(
                "select course_id from t_medication_intakes", Integer.class);
        assertThat(courseIds).containsExactly((Integer) null);
    }

    @Test
    void intakeAfterCourseDeletedSavesWithoutCourse() {
        SharedFixture fixture = sharedFixture();

        courseService.delete(fixture.ownerId(), fixture.profileId(), fixture.course().getId());

        IntakeSaveResult result = intakeService.save(fixture.profileId(), fixture.memberId(), fixture.course().getId(),
                new MedicationIntakeProperties("Амоксициллин", LocalDateTime.of(2026, 9, 20, 9, 30), 1));

        assertThat(courses.existsById(fixture.course().getId())).isFalse();
        assertThat(result.intake().getCourseId()).isNull();
        assertThat(intakeCount()).isEqualTo(1);
    }

    private SharedFixture sharedFixture() {
        Integer ownerId = createUser("Папа");
        Integer memberId = createUser("Мама");
        Integer profileId = createProfileLinked(ownerId, true);
        profiles.linkUserToProfile(memberId, profileId, UserRole.MEMBER, false);
        return new SharedFixture(ownerId, memberId, profileId, createActiveCourse(profileId, ownerId));
    }

    private record SharedFixture(Integer ownerId, Integer memberId, Integer profileId, MedicationCourse course) {
    }

    private int intakeCount() {
        return jdbcTemplate.queryForObject("select count(*) from t_medication_intakes", Integer.class);
    }

    private int dosesTaken(Integer courseId) {
        return jdbcTemplate.queryForObject(
                "select doses_taken from t_medication_courses where id = ?", Integer.class, courseId);
    }
}
