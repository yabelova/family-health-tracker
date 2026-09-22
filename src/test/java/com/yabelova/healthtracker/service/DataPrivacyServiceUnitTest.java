package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.domain.UserRole;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.ProfileRepository.ProfileParticipant;
import com.yabelova.healthtracker.repository.ProfileRepository.ProfileWithRole;
import com.yabelova.healthtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static com.yabelova.healthtracker.service.DataPrivacyService.Decision;
import static com.yabelova.healthtracker.service.DataPrivacyService.PrivacyDecision;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты плана удаления аккаунта: классификация профилей и атомарное применение.
 */
class DataPrivacyServiceUnitTest {

    private final ProfileService profileService = mock(ProfileService.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DataPrivacyService service = new DataPrivacyService(profileService, profileRepository, userRepository);

    private static User user(int id) {
        return User.builder().id(id).telegramId(id * 10L).telegramFirstName("Папа").build();
    }

    @Test
    void getAndGroupProfilesGroupsPersonalSharedAndMember() {
        when(profileRepository.findProfileWithRolesByUserId(10)).thenReturn(List.of(
                new ProfileWithRole(1, "Личный", UserRole.OWNER),
                new ProfileWithRole(2, "Дочка", UserRole.OWNER),
                new ProfileWithRole(3, "Папин", UserRole.MEMBER)));
        when(profileRepository.findProfileParticipants(List.of(1, 2))).thenReturn(List.of(
                new ProfileParticipant(1, 10, "Анна", null),
                new ProfileParticipant(2, 10, "Анна", null),
                new ProfileParticipant(2, 20, "Александр", null)));

        DataPrivacyService.ProfileGroups groups = service.getAndGroupProfiles(user(10));

        assertThat(groups.personal()).extracting(Profile::getName).containsExactly("Личный");
        assertThat(groups.ownerShared()).hasSize(1);
        assertThat(groups.ownerShared().getFirst().profile().getName()).isEqualTo("Дочка");
        assertThat(groups.ownerShared().getFirst().participants())
                .extracting(ProfileParticipant::userId).containsExactly(20);
        assertThat(groups.member()).extracting(Profile::getName).containsExactly("Папин");
    }

    @Test
    void getAndGroupProfilesWithoutProfilesReturnsEmptyGroups() {
        when(profileRepository.findProfileWithRolesByUserId(10)).thenReturn(List.of());

        DataPrivacyService.ProfileGroups groups = service.getAndGroupProfiles(user(10));

        assertThat(groups.personal()).isEmpty();
        assertThat(groups.ownerShared()).isEmpty();
        assertThat(groups.member()).isEmpty();
    }

    @Test
    void applyDeletionPlanRunsDecisionsReGroupAndDelete() {
        User user = user(10);
        // пост-состояние: после трансфера профиль 2 — MEMBER, после ревока профиль 3 — личный OWNER
        when(profileRepository.findProfileWithRolesByUserId(10)).thenReturn(List.of(
                new ProfileWithRole(2, "Дочка", UserRole.MEMBER),
                new ProfileWithRole(3, "Общий", UserRole.OWNER)));
        when(profileRepository.findProfileParticipants(List.of(3))).thenReturn(List.of(
                new ProfileParticipant(3, 10, "Лиза", null)));

        service.applyDeletionPlan(user, List.of(
                new PrivacyDecision(2, Decision.TRANSFER, 20),
                new PrivacyDecision(3, Decision.REVOKE, null)));

        InOrder order = inOrder(profileService, profileRepository, userRepository);
        order.verify(profileService).transferOwnership(user, 2, 20);
        order.verify(profileService).revokeAccess(user, 3);
        order.verify(profileRepository).findProfileWithRolesByUserId(10);
        order.verify(profileService).deleteProfile(user, 3);
        order.verify(userRepository).deleteById(10);
    }

    @Test
    void applyDeletionPlanDeletesEveryPersonalProfileFromReRead() {
        User user = user(10);
        when(profileRepository.findProfileWithRolesByUserId(10)).thenReturn(List.of(
                new ProfileWithRole(1, "Мой 1", UserRole.OWNER),
                new ProfileWithRole(2, "Мой 2", UserRole.OWNER)));
        when(profileRepository.findProfileParticipants(List.of(1, 2))).thenReturn(List.of(
                new ProfileParticipant(1, 10, "Евгения", null),
                new ProfileParticipant(2, 10, "Евгения", null)));

        service.applyDeletionPlan(user, List.of());

        verify(profileService).deleteProfile(user, 1);
        verify(profileService).deleteProfile(user, 2);
        verify(userRepository).deleteById(10);
    }

    @Test
    void applyDeletionPlanWithoutProfilesAndDecisionsOnlyDeletesUser() {
        User user = user(10);
        when(profileRepository.findProfileWithRolesByUserId(10)).thenReturn(List.of());

        service.applyDeletionPlan(user, List.of());

        verify(profileService, never()).transferOwnership(any(), any(), any());
        verify(profileService, never()).revokeAccess(any(), any());
        verify(profileService, never()).deleteProfile(any(), any());
        verify(userRepository).deleteById(10);
    }
}
