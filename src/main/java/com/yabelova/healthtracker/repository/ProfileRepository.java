package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.UserRole;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRepository extends CrudRepository<Profile, Integer> {

    @Query("""
                SELECT p.* FROM t_profiles p
                JOIN tr_user_profile up ON p.id = up.profile_id
                WHERE up.user_id = :userId
            """)
    List<Profile> findAllByUserId(@Param("userId") Long userId);

    @Query("""
                SELECT profile_id FROM tr_user_profile
                WHERE user_id = :userId AND is_active = true
            """)
    Integer findActiveProfileIdByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
                UPDATE tr_user_profile SET is_active = false
                WHERE user_id = :userId AND is_active = true
            """)
    void deactivateActive(@Param("userId") Long userId);

    @Modifying
    @Query("""
                UPDATE tr_user_profile SET is_active = true
                WHERE user_id = :userId AND profile_id = :profileId
            """)
    void activateProfile(@Param("userId") Long userId,
                         @Param("profileId") Integer profileId);

    @Modifying
    @Query("""
                INSERT INTO tr_user_profile (user_id, profile_id, role, is_active)
                VALUES (:userId, :profileId, :role, :isActive)
            """)
    void linkUserToProfile(@Param("userId") Long userId,
                           @Param("profileId") Integer profileId,
                           @Param("role") UserRole role,
                           @Param("isActive") boolean isActive);

    @Query("""
                SELECT EXISTS(
                    SELECT 1 FROM tr_user_profile
                    WHERE user_id = :userId AND profile_id = :profileId AND role = :role
                )
            """)
    boolean isLinkedAs(@Param("userId") Long userId,
                       @Param("profileId") Integer profileId,
                       @Param("role") UserRole role);

    @Modifying
    @Query("""
                DELETE FROM tr_user_profile
                WHERE profile_id = :profileId AND user_id != :userId
            """)
    void deleteLinksExceptUser(@Param("profileId") Integer profileId,
                               @Param("userId") Long userId);
}
