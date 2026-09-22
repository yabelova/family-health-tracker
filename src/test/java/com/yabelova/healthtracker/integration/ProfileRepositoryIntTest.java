package com.yabelova.healthtracker.integration;

import com.yabelova.healthtracker.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты ограничений связей профиля: единственная активность, снятие участников и каскады удаления.
 */
class ProfileRepositoryIntTest extends AbstractContainerIntTest {

    @Test
    void secondActiveLinkViolatesPartialIndex() {
        Integer userId = createUser("Папа");
        createProfileLinked(userId, true);

        assertThatThrownBy(() -> createProfileLinked(userId, true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deactivateThenActivateSwitchesActiveProfile() {
        Integer userId = createUser("Папа");
        Integer first = createProfileLinked(userId, true);
        Integer second = createProfileLinked(userId, false);

        profiles.deactivateAllActive(userId);
        profiles.activateProfile(userId, second);

        assertThat(activeProfileId(userId)).isEqualTo(second);
        assertThat(activeCount(userId)).isEqualTo(1);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void deleteParticipantLinksKeepsOwner() {
        Integer userId = createUser("Папа");
        Integer profileId = createProfileLinked(userId, true);
        Integer member = createUser("Мама");
        profiles.linkUserToProfile(member, profileId, UserRole.MEMBER, false);

        profiles.deleteParticipantLinks(profileId, userId);

        assertThat(linkCount(profileId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select exists(select 1 from tr_user_profile
                              where profile_id = ? and user_id = ?)
                """, Boolean.class, profileId, userId)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                select exists(select 1 from tr_user_profile
                              where profile_id = ? and user_id = ?)
                """, Boolean.class, profileId, member)).isFalse();
    }

    @Test
    void deletingProfileCascadesLinks() {
        Integer owner = createUser("Папа");
        Integer profileId = createProfileLinked(owner, true);
        Integer member = createUser("Мама");
        profiles.linkUserToProfile(member, profileId, UserRole.MEMBER, false);

        profiles.deleteById(profileId);

        assertThat(linkCount(profileId)).isZero();
        assertThat(profiles.existsById(profileId)).isFalse();
    }

    private Integer activeProfileId(Integer userId) {
        return jdbcTemplate.queryForObject(
                "select profile_id from tr_user_profile where user_id = ? and is_active",
                Integer.class, userId);
    }

    private int activeCount(Integer userId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from tr_user_profile where user_id = ? and is_active",
                Integer.class, userId);
    }

    private int linkCount(Integer profileId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from tr_user_profile where profile_id = ?",
                Integer.class, profileId);
    }
}
