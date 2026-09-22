package com.yabelova.healthtracker.integration;

import com.yabelova.healthtracker.domain.ProfileInvite;
import com.yabelova.healthtracker.repository.ProfileInviteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты приглашений: атомарный одноразовый claim и чистка неиспользованных.
 */
class ProfileInviteRepositoryIntTest extends AbstractContainerIntTest {

    private static final String CODE = "ABC123XY";

    @Autowired
    ProfileInviteRepository inviteRepository;

    @Test
    void claimIsAtomicOneTime() {
        Integer owner = createUser("Папа");
        Integer profileId = createProfileLinked(owner, true);
        Integer guest = createUser("Мама");
        saveInvite(profileId, CODE, Instant.now().plus(24, ChronoUnit.HOURS));

        long first = inviteRepository.claim(CODE, Instant.now(), guest);
        long second = inviteRepository.claim(CODE, Instant.now(), guest);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
    }

    @Test
    void claimRejectsExpiredInvite() {
        Integer owner = createUser("Папа");
        Integer profileId = createProfileLinked(owner, true);
        Integer guest = createUser("Мама");
        saveInvite(profileId, CODE, Instant.now().minus(1, ChronoUnit.HOURS));

        long claimed = inviteRepository.claim(CODE, Instant.now(), guest);

        assertThat(claimed).isZero();
    }

    @Test
    void deleteUnusedByProfileKeepsUsedInvites() {
        final String usedCode = "USED1234";

        Integer owner = createUser("Папа");
        Integer profileId = createProfileLinked(owner, true);
        Integer guest = createUser("Мама");
        saveInvite(profileId, CODE, Instant.now().plus(24, ChronoUnit.HOURS));
        saveInvite(profileId, usedCode, Instant.now().plus(24, ChronoUnit.HOURS));
        inviteRepository.claim(usedCode, Instant.now(), guest);

        inviteRepository.deleteUnusedByProfile(profileId);

        List<String> codes = jdbcTemplate.queryForList(
                "select code from t_profile_invites where profile_id = ?", String.class, profileId);
        assertThat(codes).containsExactly(usedCode);
    }

    private void saveInvite(Integer profileId, String code, Instant expiresAt) {
        inviteRepository.save(ProfileInvite.builder()
                .profileId(profileId)
                .code(code)
                .expiresAt(expiresAt)
                .build());
    }
}
