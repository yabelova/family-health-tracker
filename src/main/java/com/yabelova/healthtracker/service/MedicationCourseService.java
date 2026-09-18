package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationCourseService {

    /**
     * Курс остается доступным для отметки приема еще сколько дней после окончания (отметка приема задним числом).
     */
    public static final int ACTIVE_GRACE_DAYS = 7;

    private final MedicationCourseRepository repository;
    private final ProfileAccessGuard accessGuard;

    @Transactional
    public MedicationCourse save(Integer profileId, Integer createdBy, MedicationCourseProperties properties) {
        accessGuard.checkAndLock(createdBy, profileId);
        MedicationCourse course = MedicationCourse.builder()
                .profileId(profileId)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .properties(properties)
                .dosesTaken(0)
                .build();
        return repository.save(course);
    }

    /**
     * Все курсы профиля.
     */
    public List<MedicationCourse> listByProfile(Integer userId, Integer profileId) {
        accessGuard.check(userId, profileId);
        return repository.findByProfileId(profileId);
    }

    /**
     * Курсы профиля для экспорта: активные и будущие.
     */
    public List<MedicationCourse> listActiveOrUpcoming(Integer userId, Integer profileId, LocalDate day) {
        accessGuard.check(userId, profileId);
        return repository.findActiveOrUpcomingByProfileId(profileId, day);
    }

    /**
     * Курсы профиля для кнопок приема: активные на {@code day} + завершившиеся в пределах {@link ACTIVE_GRACE_DAYS} дней
     * (отметка приема задним числом).
     */
    public List<MedicationCourse> listIntakeOptions(Integer userId, Integer profileId, LocalDate day) {
        accessGuard.check(userId, profileId);
        return repository.findIntakeOptionsByProfileId(profileId, day, ACTIVE_GRACE_DAYS);
    }

    /**
     * Курс для заполнения анкеты приема после клика по кнопке.
     * Без гарда - проверки будут выполнены в конце визарда при подтверждении.
     */
    public MedicationCourse findForIntakeName(Integer courseId) {
        return repository.findById(courseId).orElse(null);
    }

    @Transactional
    public void delete(Integer userId, Integer profileId, Integer id) {
        accessGuard.checkAndLock(userId, profileId);
        MedicationCourse course = repository.findById(id)
                .orElseThrow(() -> new RecordOperationException(RecordOperationException.Error.RECORD_NOT_FOUND));
        if (!course.getProfileId().equals(profileId)) {
            throw new RecordOperationException(RecordOperationException.Error.RECORD_NOT_LINKED);
        }
        repository.deleteById(id);
    }
}
