package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.MedicationCourse;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationCourseRepository extends ListCrudRepository<MedicationCourse, Integer> {

    List<MedicationCourse> findByProfileId(Integer profileId);

    /**
     * Курсы для экспорта: активные на {@code day} + еще не начавшиеся (будущие).
     * Курс без даты начала или без количества дней считается бессрочным.
     */
    @Query("""
                SELECT * FROM t_medication_courses
                WHERE profile_id = :profileId
                  AND (properties ->> 'startDate' IS NULL
                       OR properties ->> 'daysCount' IS NULL
                       OR CAST(properties ->> 'startDate' AS date) + CAST(properties ->> 'daysCount' AS integer) >= CAST(:day AS date))
            """)
    List<MedicationCourse> findActiveOrUpcomingByProfileId(@Param("profileId") Integer profileId,
                                                           @Param("day") LocalDate day);

    /**
     * Курсы для кнопок приема: активные на {@code day} + завершившиеся в пределах {@code graceDays} дней
     * (отметка приема задним числом).
     * Курс без даты начала или без количества дней считается бессрочным.
     */
    @Query("""
                SELECT * FROM t_medication_courses
                WHERE profile_id = :profileId
                  AND (properties ->> 'startDate' IS NULL
                       OR CAST(properties ->> 'startDate' AS date) <= CAST(:day AS date))
                  AND (properties ->> 'daysCount' IS NULL
                       OR CAST(properties ->> 'startDate' AS date) + CAST(properties ->> 'daysCount' AS integer) >= CAST(:day AS date) - :graceDays)
            """)
    List<MedicationCourse> findIntakeOptionsByProfileId(@Param("profileId") Integer profileId,
                                                        @Param("day") LocalDate day,
                                                        @Param("graceDays") Integer graceDays);

    /**
     * Курс для приема на конкретную дату: должен принадлежать профилю и быть активным на {@code day}
     * с учетом {@code graceDays} (для отметки задним числом).
     * Пусто, если курс не найден, чужой или неактивен на дату приема.
     * Строка курса блокируется до конца транзакции приема: параллельное удаление курса ждет коммита.
     */
    @Query("""
                SELECT * FROM t_medication_courses
                WHERE id = :id AND profile_id = :profileId
                  AND (properties ->> 'startDate' IS NULL
                       OR CAST(properties ->> 'startDate' AS date) <= CAST(:day AS date))
                  AND (properties ->> 'daysCount' IS NULL
                       OR CAST(properties ->> 'startDate' AS date) + CAST(properties ->> 'daysCount' AS integer) >= CAST(:day AS date) - :graceDays)
                LIMIT 1
                FOR UPDATE
            """)
    Optional<MedicationCourse> findIntakeCourseForDateWithLock(@Param("id") Integer id,
                                                               @Param("profileId") Integer profileId,
                                                               @Param("day") LocalDate day,
                                                               @Param("graceDays") Integer graceDays);

    /**
     * Курсы для плана на сегодня: активные строго на {@code day}, для всех получателей плана одним запросом.
     */
    @Query("""
                SELECT up.user_id AS user_id,
                       p.id AS profile_id,
                       p.name AS profile_name,
                       c.properties ->> 'medication' AS medication,
                       CAST(c.properties ->> 'dosesPerDay' AS integer) AS doses_per_day
                FROM t_profiles p
                JOIN tr_user_profile up ON up.profile_id = p.id
                JOIN t_medication_courses c ON c.profile_id = p.id
                WHERE up.user_id IN (:userIds)
                  AND (c.properties ->> 'startDate' IS NULL
                       OR c.properties ->> 'daysCount' IS NULL
                       OR (CAST(c.properties ->> 'startDate' AS date) <= CAST(:day AS date)
                           AND CAST(c.properties ->> 'startDate' AS date) + CAST(c.properties ->> 'daysCount' AS integer) > CAST(:day AS date)))
                ORDER BY up.user_id, p.id, c.id
            """)
    List<MedicationCoursePlanRow> findMedicationCoursePlanRowsByUserIds(@Param("userIds") Collection<Integer> userIds,
                                                                        @Param("day") LocalDate day);

    record MedicationCoursePlanRow(Integer userId, Integer profileId, String profileName, String medication,
                                   Integer dosesPerDay) {
    }

    /**
     * Атомарный инкремент счетчика принятых доз.
     */
    @Modifying
    @Query("""
                UPDATE t_medication_courses
                SET doses_taken = doses_taken + :doses
                WHERE id = :id
            """)
    void addDosesTaken(@Param("id") Integer id, @Param("doses") Integer doses);

    /**
     * Атомарный декремент при удалении приема (в ноль не уходит).
     */
    @Modifying
    @Query("""
                UPDATE t_medication_courses
                SET doses_taken = GREATEST(0, doses_taken - :doses)
                WHERE id = :id
            """)
    void subtractDosesTaken(@Param("id") Integer id, @Param("doses") Integer doses);
}
