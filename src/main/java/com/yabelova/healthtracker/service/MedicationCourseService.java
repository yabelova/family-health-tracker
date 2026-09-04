package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationCourseService {

    private final MedicationCourseRepository repository;
    private final ProfileAccessGuard accessGuard;

    public MedicationCourse save(Integer profileId, Long createdBy, MedicationCourseProperties properties) {
        accessGuard.check(createdBy, profileId);
        MedicationCourse course = MedicationCourse.builder()
                .profileId(profileId)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .properties(properties)
                .remainingDoses(properties.getDosesPerPackage())
                .build();
        return repository.save(course);
    }

    public List<MedicationCourse> listByProfile(Long userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileId(profileId);
    }

    public void delete(Long userId, Integer profileId, Integer id) {
        accessGuard.check(userId, profileId);
        MedicationCourse course = repository.findById(id)
                .orElseThrow(() -> new RecordOperationException(RecordOperationException.Error.RECORD_NOT_FOUND));
        if (!course.getProfileId().equals(profileId)) {
            throw new RecordOperationException(RecordOperationException.Error.RECORD_NOT_LINKED);
        }
        repository.deleteById(id);
    }
}
