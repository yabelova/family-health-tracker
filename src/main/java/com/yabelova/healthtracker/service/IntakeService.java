package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationIntake;
import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import com.yabelova.healthtracker.repository.MedicationIntakeRepository;
import com.yabelova.healthtracker.util.TimeZones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntakeService {

    private final MedicationIntakeRepository repository;
    private final MedicationCourseRepository courseRepository;
    private final ProfileAccessGuard accessGuard;

    /**
     * Сохраняет прием и увеличивает счетчик доз курса.
     */
    @Transactional
    public MedicationIntake save(Integer profileId, Integer createdBy, Integer courseId,
                                 MedicationIntakeProperties properties) {
        accessGuard.check(createdBy, profileId);

        MedicationCourse course = findCourseForProfile(courseId, profileId);
        MedicationIntake intake = MedicationIntake.builder()
                .profileId(profileId)
                .courseId(course != null ? courseId : null)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .properties(properties)
                .build();

        intake = repository.save(intake);

        if (course != null) {
            courseRepository.addDosesTaken(course.getId(), properties.getDoses());
        }
        return intake;
    }

    public List<MedicationIntake> listByProfile(Integer userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileId(profileId);
    }

    public List<MedicationIntake> listLastSevenDays(Integer userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileIdAndTakenAtSince(profileId,
                LocalDate.now(TimeZones.DEFAULT).minusDays(7).atStartOfDay());
    }

    public List<MedicationIntake> listToday(Integer userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileIdAndTakenAtSince(profileId,
                LocalDate.now(TimeZones.DEFAULT).atStartOfDay());
    }

    @Transactional
    public void delete(Integer userId, Integer profileId, Integer id) {
        accessGuard.check(userId, profileId);
        MedicationIntake intake = repository.findById(id)
                .orElseThrow(() -> new RecordOperationException(RecordOperationException.Error.RECORD_NOT_FOUND));
        if (!intake.getProfileId().equals(profileId)) {
            throw new RecordOperationException(RecordOperationException.Error.RECORD_NOT_LINKED);
        }
        repository.deleteById(id);

        if (intake.getCourseId() != null
                && intake.getProperties() != null && intake.getProperties().getDoses() != null) {
            courseRepository.subtractDosesTaken(intake.getCourseId(), intake.getProperties().getDoses());
        }
    }

    private MedicationCourse findCourseForProfile(Integer courseId, Integer profileId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId)
                .filter(course -> course.getProfileId().equals(profileId))
                .orElse(null);
    }
}
