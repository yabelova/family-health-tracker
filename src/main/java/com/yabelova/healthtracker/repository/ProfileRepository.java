package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.UserRole;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProfileRepository extends ListCrudRepository<Profile, Integer> {

    @Query("""
                SELECT p.* FROM t_profiles p
                JOIN tr_user_profile up ON p.id = up.profile_id
                WHERE up.user_id = :userId
            """)
    List<Profile> findAllByUserId(@Param("userId") Integer userId);

    @Query("""
                SELECT profile_id FROM tr_user_profile
                WHERE user_id = :userId AND is_active = true
            """)
    Integer findActiveProfileIdByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("""
                UPDATE tr_user_profile SET is_active = false
                WHERE user_id = :userId AND is_active = true
            """)
    void deactivateAllActive(@Param("userId") Integer userId);

    @Modifying
    @Query("""
                UPDATE tr_user_profile SET is_active = true
                WHERE user_id = :userId AND profile_id = :profileId
            """)
    void activateProfile(@Param("userId") Integer userId,
                         @Param("profileId") Integer profileId);

    @Modifying
    @Query("""
                INSERT INTO tr_user_profile (user_id, profile_id, role, is_active)
                VALUES (:userId, :profileId, :role, :isActive)
            """)
    void linkUserToProfile(@Param("userId") Integer userId,
                           @Param("profileId") Integer profileId,
                           @Param("role") UserRole role,
                           @Param("isActive") boolean isActive);

    @Query("""
                SELECT EXISTS(
                    SELECT 1 FROM tr_user_profile
                    WHERE user_id = :userId AND profile_id = :profileId AND role = :role
                )
            """)
    boolean isLinkedAs(@Param("userId") Integer userId,
                       @Param("profileId") Integer profileId,
                       @Param("role") UserRole role);

    /**
     * Проверка, что профиль доступен юзеру.
     */
    @Query("""
                SELECT EXISTS(
                    SELECT 1 FROM tr_user_profile
                    WHERE user_id = :userId AND profile_id = :profileId
                )
            """)
    boolean isLinked(@Param("userId") Integer userId,
                     @Param("profileId") Integer profileId);

    /**
     * Проверка, что профиль доступен юзеру, с блокировкой строки внутри транзакции записи.
     */
    @Query("""
                SELECT EXISTS(
                    SELECT 1 FROM tr_user_profile
                    WHERE user_id = :userId AND profile_id = :profileId
                    FOR UPDATE
                )
            """)
    boolean isLinkedWithLock(@Param("userId") Integer userId,
                             @Param("profileId") Integer profileId);

    @Modifying
    @Query("""
                DELETE FROM tr_user_profile
                WHERE profile_id = :profileId AND user_id != :userId
            """)
    void deleteParticipantLinks(@Param("profileId") Integer profileId,
                                @Param("userId") Integer userId);

    @Query("""
                SELECT count(*) FROM tr_user_profile
                WHERE profile_id = :profileId AND user_id != :userId
            """)
    long countOtherParticipants(@Param("profileId") Integer profileId,
                                @Param("userId") Integer userId);

    @Modifying
    @Query("""
                UPDATE tr_user_profile
                SET role = CASE
                    WHEN user_id = :fromUserId THEN 'MEMBER'
                    WHEN user_id = :toUserId THEN 'OWNER'
                    ELSE role
                END
                WHERE profile_id = :profileId
                  AND user_id IN (:fromUserId, :toUserId)
            """)
    void transferRole(@Param("profileId") Integer profileId,
                      @Param("fromUserId") Integer fromUserId,
                      @Param("toUserId") Integer toUserId);

    @Query("""
                SELECT p.id as profile_id, p.name, up.role
                FROM t_profiles p
                JOIN tr_user_profile up ON up.profile_id = p.id
                WHERE up.user_id = :userId
            """)
    List<ProfileWithRole> findProfileWithRolesByUserId(@Param("userId") Integer userId);

    @Query("""
                SELECT up.profile_id, up.user_id, u.telegram_first_name, u.telegram_username
                FROM tr_user_profile up
                JOIN t_users u ON u.id = up.user_id
                WHERE up.profile_id IN (:profileIds)
                ORDER BY u.telegram_first_name
            """)
    List<ProfileParticipant> findProfileParticipants(
            @Param("profileIds") Collection<Integer> profileIds);

    record ProfileWithRole(Integer profileId, String name, UserRole role) {
    }

    record ProfileParticipant(Integer profileId, Integer userId, String telegramFirstName, String telegramUsername) {
    }
}
