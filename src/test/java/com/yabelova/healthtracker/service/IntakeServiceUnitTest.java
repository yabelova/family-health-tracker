package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.domain.MedicationIntake;
import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import com.yabelova.healthtracker.repository.MedicationIntakeRepository;
import com.yabelova.healthtracker.service.IntakeService.IntakeSaveResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты сохранения приема: проверка доступа, привязка активного курса и результат.
 */
class IntakeServiceUnitTest {

    private final MedicationIntakeRepository intakeRepository = mock(MedicationIntakeRepository.class);
    private final MedicationCourseRepository courseRepository = mock(MedicationCourseRepository.class);
    private final ProfileAccessGuard accessGuard = mock(ProfileAccessGuard.class);
    private final IntakeService service = new IntakeService(intakeRepository, courseRepository, accessGuard);

    private static final LocalDateTime TAKEN_AT = LocalDateTime.of(2026, 9, 18, 8, 0);
    private static final LocalDate TAKEN_DAY = TAKEN_AT.toLocalDate();

    @Test
    void rejectsWhenAccessRevoked() {
        doThrow(new RecordOperationException(
                RecordOperationException.Error.PROFILE_ACCESS_DENIED))
                .when(accessGuard).checkAndLock(10, 1);

        assertThatThrownBy(() -> service.save(1, 10, 5, properties()))
                .isInstanceOf(RecordOperationException.class);
        verify(intakeRepository, never()).save(any(MedicationIntake.class));
    }

    @Test
    void linksCourseActiveOnTakenDateAndIncrementsDoses() {
        MedicationCourse course = course(5);
        when(courseRepository.findIntakeCourseForDateWithLock(eq(5), eq(1), eq(TAKEN_DAY),
                eq(MedicationCourseService.ACTIVE_GRACE_DAYS))).thenReturn(Optional.of(course));
        when(intakeRepository.save(any(MedicationIntake.class))).thenAnswer(inv -> inv.getArgument(0));

        IntakeSaveResult result = service.save(1, 10, 5, properties());

        verify(accessGuard).checkAndLock(10, 1);
        assertThat(result.course()).isSameAs(course);
        assertThat(result.course().getDosesTaken()).isEqualTo(2);
        assertThat(result.intake().getCourseId()).isEqualTo(5);
        verify(courseRepository).addDosesTaken(5, 2);
    }

    @Test
    void courseGoneOrNotActiveOnTakenDateSavesAsManual() {
        when(courseRepository.findIntakeCourseForDateWithLock(eq(5), eq(1), eq(TAKEN_DAY),
                eq(MedicationCourseService.ACTIVE_GRACE_DAYS))).thenReturn(Optional.empty());
        when(intakeRepository.save(any(MedicationIntake.class))).thenAnswer(inv -> inv.getArgument(0));

        IntakeSaveResult result = service.save(1, 10, 5, properties());

        assertThat(result.course()).isNull();
        assertThat(result.intake().getCourseId()).isNull();
        verify(courseRepository, never()).addDosesTaken(any(), any());
    }

    @Test
    void manualIntakeWithoutCourseIdSavesUnlinked() {
        when(intakeRepository.save(any(MedicationIntake.class))).thenAnswer(inv -> inv.getArgument(0));

        IntakeSaveResult result = service.save(1, 10, null, properties());

        assertThat(result.course()).isNull();
        assertThat(result.intake().getCourseId()).isNull();
        verify(courseRepository, never()).findIntakeCourseForDateWithLock(any(), any(), any(), any());
        verify(courseRepository, never()).addDosesTaken(any(), any());
    }

    private MedicationIntakeProperties properties() {
        return new MedicationIntakeProperties("Антибиотик", TAKEN_AT, 2);
    }

    private MedicationCourse course(Integer id) {
        MedicationCourseProperties properties = new MedicationCourseProperties();
        properties.setMedication("Антибиотик");
        properties.setDosesPerDay(2);
        return MedicationCourse.builder()
                .id(id)
                .profileId(1)
                .properties(properties)
                .dosesTaken(0)
                .build();
    }
}
