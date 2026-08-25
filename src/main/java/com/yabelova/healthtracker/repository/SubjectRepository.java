package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.Subject;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends CrudRepository<Subject, Integer> {

    @Query("""
                SELECT s.* FROM t_subjects s
                JOIN tr_user_subject us ON s.id = us.subject_id
                WHERE us.user_id = :userId
            """)
    List<Subject> findAllByUserId(@Param("userId") Long userId);
}
