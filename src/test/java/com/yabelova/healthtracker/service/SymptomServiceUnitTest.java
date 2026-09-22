package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.SymptomLog;
import com.yabelova.healthtracker.domain.SymptomLogProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.SymptomLogRepository;
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
 * Тесты записи симптома: права на запись и удаление, связь с профилем.
 */
class SymptomServiceUnitTest {

    private final SymptomLogRepository repository = mock(SymptomLogRepository.class);
    private final ProfileAccessGuard accessGuard = mock(ProfileAccessGuard.class);
    private final SymptomService service = new SymptomService(repository, accessGuard);

    @Test
    void saveBuildsLogWithOwnership() {
        SymptomLogProperties properties = new SymptomLogProperties();
        properties.setDescription("Температура 37,2");
        when(repository.save(any(SymptomLog.class))).thenAnswer(inv -> inv.getArgument(0));

        SymptomLog saved = service.save(1, 10, properties);

        assertThat(saved.getProfileId()).isEqualTo(1);
        assertThat(saved.getCreatedBy()).isEqualTo(10);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getProperties()).isSameAs(properties);
        verify(accessGuard).checkAndLock(10, 1);
    }

    @Test
    void saveRejectsAccessWithoutPersisting() {
        doThrow(new RecordOperationException(RecordOperationException.Error.PROFILE_ACCESS_DENIED))
                .when(accessGuard).checkAndLock(10, 1);

        assertThatThrownBy(() -> service.save(1, 10, new SymptomLogProperties()))
                .isInstanceOfSatisfying(RecordOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(RecordOperationException.Error.PROFILE_ACCESS_DENIED));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesLinkedLog() {
        SymptomLog log = SymptomLog.builder().id(5).profileId(1).createdBy(10).build();
        when(repository.findById(5)).thenReturn(Optional.of(log));

        service.delete(10, 1, 5);

        verify(repository).deleteById(5);
    }

    @Test
    void deleteRejectsLogOfAnotherProfile() {
        SymptomLog log = SymptomLog.builder().id(5).profileId(2).createdBy(10).build();
        when(repository.findById(5)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> service.delete(10, 1, 5))
                .isInstanceOfSatisfying(RecordOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(RecordOperationException.Error.RECORD_NOT_LINKED));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsMissingLog() {
        when(repository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10, 1, 5))
                .isInstanceOfSatisfying(RecordOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(RecordOperationException.Error.RECORD_NOT_FOUND));
        verify(repository, never()).deleteById(any());
    }
}
