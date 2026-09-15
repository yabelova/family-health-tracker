package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.MedicationCourse;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@SuppressWarnings("NullableProblems")
public interface MedicationCourseRepository extends ListCrudRepository<MedicationCourse, Integer> {

    List<MedicationCourse> findByProfileId(Integer profileId);

    /**
     * Атомарный инкремент счетчика принятых доз
     */
    @Modifying
    @Query("""
                UPDATE t_medication_courses
                SET doses_taken = doses_taken + :doses
                WHERE id = :id
            """)
    void addDosesTaken(@Param("id") Integer id, @Param("doses") Integer doses);

    /**
     * Атомарный декремент при удалении приема (в ноль не уходит)
     */
    @Modifying
    @Query("""
                UPDATE t_medication_courses
                SET doses_taken = GREATEST(0, doses_taken - :doses)
                WHERE id = :id
            """)
    void subtractDosesTaken(@Param("id") Integer id, @Param("doses") Integer doses);
}
