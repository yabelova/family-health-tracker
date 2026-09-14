package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.MedicationCourse;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@SuppressWarnings("NullableProblems")
public interface MedicationCourseRepository extends ListCrudRepository<MedicationCourse, Integer> {

    List<MedicationCourse> findByProfileId(Integer profileId);
}
