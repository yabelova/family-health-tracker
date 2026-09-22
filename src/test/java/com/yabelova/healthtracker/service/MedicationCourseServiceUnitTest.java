package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты сервиса курсов: быстрое чтение для кнопки приема без проверки доступа, создание и удаление с проверкой прав.
 */
class MedicationCourseServiceUnitTest {

    private final MedicationCourseRepository repository = mock(MedicationCourseRepository.class);
    private final ProfileAccessGuard accessGuard = mock(ProfileAccessGuard.class);
    private final MedicationCourseService service = new MedicationCourseService(repository, accessGuard);

    @Test
    void findForIntakeNameReturnsCourseOfProfile() {
        MedicationCourse course = MedicationCourse.builder().id(5).profileId(1).build();
        when(repository.findById(5)).thenReturn(Optional.of(course));

        assertThat(service.findForIntakeName(5)).isSameAs(course);
    }

    @Test
    void findForIntakeNameReturnsNullWhenMissing() {
        when(repository.findById(5)).thenReturn(Optional.empty());

        assertThat(service.findForIntakeName(5)).isNull();
    }

    @Test
    void findForIntakeNameDoesNotCheckAccess() {
        MedicationCourse course = MedicationCourse.builder().id(5).profileId(1).build();
        when(repository.findById(5)).thenReturn(Optional.of(course));

        service.findForIntakeName(5);

        verify(accessGuard, never()).check(any(), any());
        verify(accessGuard, never()).checkAndLock(any(), any());
    }

    @Test
    void saveBuildsCourseWithZeroDosesTaken() {
        MedicationCourseProperties properties = new MedicationCourseProperties();
        properties.setMedication("Амоксициллин");
        when(repository.save(any(MedicationCourse.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicationCourse saved = service.save(1, 10, properties);

        assertThat(saved.getProfileId()).isEqualTo(1);
        assertThat(saved.getCreatedBy()).isEqualTo(10);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getDosesTaken()).isZero();
        assertThat(saved.getProperties()).isSameAs(properties);
        verify(accessGuard).checkAndLock(10, 1);
    }

    @Test
    void saveRejectsAccessWithoutPersisting() {
        MedicationCourseProperties properties = new MedicationCourseProperties();
        doThrow(new RecordOperationException(RecordOperationException.Error.PROFILE_ACCESS_DENIED))
                .when(accessGuard).checkAndLock(10, 1);

        assertThatThrownBy(() -> service.save(1, 10, properties))
                .isInstanceOfSatisfying(RecordOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(RecordOperationException.Error.PROFILE_ACCESS_DENIED));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesLinkedCourse() {
        MedicationCourse course = MedicationCourse.builder().id(5).profileId(1).createdBy(10).build();
        when(repository.findById(5)).thenReturn(Optional.of(course));

        service.delete(10, 1, 5);

        verify(repository).deleteById(5);
        verify(accessGuard).checkAndLock(10, 1);
    }

    @Test
    void deleteRejectsCourseOfAnotherProfile() {
        MedicationCourse course = MedicationCourse.builder().id(5).profileId(2).createdBy(10).build();
        when(repository.findById(5)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.delete(10, 1, 5))
                .isInstanceOfSatisfying(RecordOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(RecordOperationException.Error.RECORD_NOT_LINKED));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsMissingCourse() {
        when(repository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10, 1, 5))
                .isInstanceOfSatisfying(RecordOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(RecordOperationException.Error.RECORD_NOT_FOUND));
        verify(repository, never()).deleteById(any());
    }
}
