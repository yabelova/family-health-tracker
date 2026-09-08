package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import com.yabelova.healthtracker.util.TimeZones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationCourseService {

    /**
     * Курс остается доступным для отметки приема еще сколько дней после окончания
     * (прием задним числом).
     */
    private static final long ACTIVE_GRACE_DAYS = 7;

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

    /**
     * Активные курсы профиля: без ограничения дней, с еще не завершившимся курсом
     * или завершившимся в пределах {@link #ACTIVE_GRACE_DAYS} (чтобы можно было
     * отметить прием задним числом).
     */
    public List<MedicationCourse> listActiveByProfile(Long userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileId(profileId).stream()
                .filter(MedicationCourseService::isActive)
                .toList();
    }

    /**
     * Активный курс по ID: должен существовать, принадлежать профилю и быть активным.
     * Возвращает {@code null}, если курс не найден, не для этого профиля или завершен.
     */
    public MedicationCourse findActiveForProfileById(Long userId, Integer profileId, Integer id) {
        accessGuard.check(userId, profileId);
        MedicationCourse course = repository.findById(id).orElse(null);
        if (course == null || !course.getProfileId().equals(profileId) || !isActive(course)) {
            return null;
        }
        return course;
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

    private static boolean isActive(MedicationCourse course) {
        MedicationCourseProperties properties = course.getProperties();
        if (properties == null || properties.getDaysCount() == null) {
            return true;
        }
        LocalDate startDate = properties.getStartDate();
        if (startDate == null) {
            return true;
        }
        LocalDate endDate = startDate.plusDays(properties.getDaysCount());
        return !endDate.isBefore(LocalDate.now(TimeZones.DEFAULT).minusDays(ACTIVE_GRACE_DAYS));
    }
}
