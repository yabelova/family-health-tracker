package com.yabelova.healthtracker.integration;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.domain.UserRole;
import com.yabelova.healthtracker.repository.ProfileRepository.ProfileParticipant;
import com.yabelova.healthtracker.service.DataPrivacyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.yabelova.healthtracker.service.DataPrivacyService.Decision;
import static com.yabelova.healthtracker.service.DataPrivacyService.PrivacyDecision;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты применения плана удаления аккаунта: отзыв, передача владения и личные профили.
 */
class DataPrivacyServiceIntTest extends AbstractContainerIntTest {

    @Autowired
    DataPrivacyService dataPrivacyService;

    @Test
    void applyDeletionPlanRevokeDeletesSharedProfileAndUser() {
        Integer ownerId = createUser("Папа");
        Integer sharedId = createProfileLinked(ownerId, false);
        Integer guestId = createUser("Мама");
        profiles.linkUserToProfile(guestId, sharedId, UserRole.MEMBER, false);

        dataPrivacyService.applyDeletionPlan(user(ownerId),
                List.of(new PrivacyDecision(sharedId, Decision.REVOKE, null)));

        assertThat(users.findById(ownerId)).isEmpty();
        assertThat(users.findById(guestId)).isNotEmpty();
        assertThat(profiles.findById(sharedId)).isEmpty();
        assertThat(profiles.findProfileWithRolesByUserId(guestId)).isEmpty();
    }

    @Test
    void applyDeletionPlanTransferOwnershipKeepsProfileOwnedByTarget() {
        Integer ownerId = createUser("Папа");
        Integer sharedId = createProfileLinked(ownerId, false);
        Integer targetId = createUser("Мама");
        profiles.linkUserToProfile(targetId, sharedId, UserRole.MEMBER, false);

        dataPrivacyService.applyDeletionPlan(user(ownerId),
                List.of(new PrivacyDecision(sharedId, Decision.TRANSFER, targetId)));

        assertThat(users.findById(ownerId)).isEmpty();
        assertThat(profiles.findById(sharedId)).isNotEmpty();
        assertThat(profiles.isLinkedAs(targetId, sharedId, UserRole.OWNER)).isTrue();
    }

    @Test
    void applyDeletionPlanDeletesOnlyUserDataKeepingForeignSharedProfile() {
        Integer ownerId = createUser("Мама");
        Integer personalId = createProfileLinked(ownerId, false);
        Integer friendId = createUser("Подруга");
        Integer friendSharedId = createProfileLinked(friendId, false);
        profiles.linkUserToProfile(ownerId, friendSharedId, UserRole.MEMBER, false);

        dataPrivacyService.applyDeletionPlan(user(ownerId), List.of());

        assertThat(users.findById(ownerId)).isEmpty();
        assertThat(profiles.findById(personalId)).isEmpty();
        assertThat(profiles.findById(friendSharedId)).isNotEmpty();
        assertThat(profiles.isLinkedAs(friendId, friendSharedId, UserRole.OWNER)).isTrue();
        assertThat(profiles.findProfileParticipants(List.of(friendSharedId)))
                .extracting(ProfileParticipant::userId)
                .containsExactly(friendId);
    }

    private User user(Integer id) {
        return users.findById(id).orElseThrow();
    }
}
