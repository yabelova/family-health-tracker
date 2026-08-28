package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.ProfileInvite;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ProfileInviteRepository extends CrudRepository<ProfileInvite, Integer> {

    Optional<ProfileInvite> findByCode(String code);

    @Modifying
    @Query("""
                UPDATE t_profile_invites SET used_at = :now, used_by = :userId
                WHERE code = :code AND used_at IS NULL AND expires_at > :now
            """)
    long claim(@Param("code") String code,
               @Param("now") Instant now,
               @Param("userId") Long userId);

    @Modifying
    @Query("""
                DELETE FROM t_profile_invites
                WHERE profile_id = :profileId AND used_at IS NULL
            """)
    long deleteUnusedByProfile(@Param("profileId") Integer profileId);
}
