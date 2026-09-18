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
     * Сохраняет прием и увеличивает счетчик доз курса, если курс активен на дату приема.
     */
    @Transactional
    public IntakeSaveResult save(Integer profileId, Integer createdBy, Integer courseId,
                                 MedicationIntakeProperties properties) {
        accessGuard.checkAndLock(createdBy, profileId);

        MedicationCourse course = findCourseForProfile(courseId, profileId, properties.getTakenAt().toLocalDate());
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
            course.setDosesTaken((course.getDosesTaken() == null ? 0 : course.getDosesTaken())
                    + properties.getDoses());
        }
        return new IntakeSaveResult(intake, course);
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
        accessGuard.checkAndLock(userId, profileId);
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

    /**
     * Резолв курса для приема с блокировкой строки внутри транзакции записи: параллельное удаление курса ждет
     * завершения записи вместо падения по FK.
     */
    private MedicationCourse findCourseForProfile(Integer courseId, Integer profileId, LocalDate day) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findIntakeCourseForDateWithLock(courseId, profileId, day,
                MedicationCourseService.ACTIVE_GRACE_DAYS)
                .orElse(null);
    }

    public record IntakeSaveResult(MedicationIntake intake, MedicationCourse course) {
    }
}
