package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.SymptomLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@SuppressWarnings("NullableProblems")
public interface SymptomLogRepository extends CrudRepository<SymptomLog, Integer> {

    List<SymptomLog> findByProfileId(Integer profileId);

    List<SymptomLog> findByProfileIdAndCreatedAtAfter(Integer profileId, Instant after);
}
