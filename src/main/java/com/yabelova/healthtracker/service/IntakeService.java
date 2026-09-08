package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationIntake;
import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.MedicationIntakeRepository;
import com.yabelova.healthtracker.util.TimeZones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntakeService {

    private final MedicationIntakeRepository repository;
    private final ProfileAccessGuard accessGuard;

    public MedicationIntake save(Integer profileId, Long createdBy, Integer courseId,
                                 MedicationIntakeProperties properties) {
        accessGuard.check(createdBy, profileId);
        MedicationIntake intake = MedicationIntake.builder()
                .profileId(profileId)
                .courseId(courseId)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .properties(properties)
                .build();
        return repository.save(intake);
    }

    public List<MedicationIntake> listByProfile(Long userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileId(profileId);
    }

    public List<MedicationIntake> listLastSevenDays(Long userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileIdAndTakenAtSince(profileId,
                LocalDate.now(TimeZones.DEFAULT).minusDays(7).atStartOfDay());
    }

    public List<MedicationIntake> listToday(Long userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileIdAndTakenAtSince(profileId,
                LocalDate.now(TimeZones.DEFAULT).atStartOfDay());
    }

    public void delete(Long userId, Integer profileId, Integer id) {
        accessGuard.check(userId, profileId);
        MedicationIntake intake = repository.findById(id)
                .orElseThrow(() -> new RecordOperationException(RecordOperationException.Error.RECORD_NOT_FOUND));
        if (!intake.getProfileId().equals(profileId)) {
            throw new RecordOperationException(RecordOperationException.Error.RECORD_NOT_LINKED);
        }
        repository.deleteById(id);
    }
}
