package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.SymptomLog;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@SuppressWarnings("NullableProblems")
public interface SymptomLogRepository extends CrudRepository<SymptomLog, Integer> {

    List<SymptomLog> findByProfileId(Integer profileId);

    @Query("""
                SELECT * FROM t_symptom_logs
                WHERE profile_id = :profileId
                  AND (properties ->> 'symptomTime')::timestamp >= :since
            """)
    List<SymptomLog> findByProfileIdAndSymptomTimeSince(@Param("profileId") Integer profileId,
                                                        @Param("since") LocalDateTime since);
}
