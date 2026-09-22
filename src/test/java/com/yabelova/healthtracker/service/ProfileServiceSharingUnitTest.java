package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.ProfileInvite;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.domain.UserRole;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.repository.ProfileInviteRepository;
import com.yabelova.healthtracker.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.yabelova.healthtracker.exception.ProfileOperationException.Error;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты шеринга профилей: приглашения, отзыв доступа и передача владения.
 */
class ProfileServiceSharingUnitTest {

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final String CODE = "ABC123XY";

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final ProfileInviteRepository inviteRepository = mock(ProfileInviteRepository.class);
    private final ProfileService service = new ProfileService(profileRepository, inviteRepository);

    private static User user() {
        return User.builder().id(10).telegramId(10 * 10L).telegramFirstName("Пользователь").build();
    }

    private static Profile profile() {
        return Profile.builder().id(1).name("Профиль " + 1).build();
    }

    private static ProfileInvite invite(Instant expiresAt) {
        return ProfileInvite.builder().profileId(1).expiresAt(expiresAt).build();
    }

    @Test
    void claimInviteNormalizesAndLinksMember() {
        User user = user();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        ProfileInvite invite = ProfileInvite.builder().profileId(1).code(CODE).expiresAt(expiresAt).build();
        Profile expected = profile();
        when(inviteRepository.findByCode(CODE)).thenReturn(Optional.of(invite));
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);
        when(profileRepository.findAllByUserId(10)).thenReturn(List.of());
        when(inviteRepository.claim(eq(CODE), any(Instant.class), eq(10))).thenReturn(1L);
        when(profileRepository.findById(1)).thenReturn(Optional.of(expected));

        Profile result = service.claimInvite(user, " abc123xy ");

        assertThat(result).isSameAs(expected);
        verify(profileRepository).linkUserToProfile(10, 1, UserRole.MEMBER, false);
    }

    @Test
    void claimInviteRejectsUnknownCode() {
        when(inviteRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.claimInvite(user(), " nope "))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.INVITE_NOT_FOUND));
    }

    @Test
    void claimInviteRejectsUsedInvite() {
        ProfileInvite invite = ProfileInvite.builder().profileId(1)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .usedAt(Instant.now()).usedBy(99).build();
        when(inviteRepository.findByCode(CODE)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.claimInvite(user(), CODE))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.INVITE_USED));
    }

    @Test
    void claimInviteRejectsExpiredInvite() {
        ProfileInvite invite = invite(Instant.now().minus(1, ChronoUnit.HOURS));
        when(inviteRepository.findByCode(CODE)).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.claimInvite(user(), CODE))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.INVITE_EXPIRED));
    }

    @Test
    void claimInviteRejectsOwnProfile() {
        ProfileInvite invite = invite(Instant.now().plus(1, ChronoUnit.HOURS));
        when(inviteRepository.findByCode(CODE)).thenReturn(Optional.of(invite));
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(true);

        assertThatThrownBy(() -> service.claimInvite(user(), CODE))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.OWN_PROFILE));
        verify(inviteRepository, never()).claim(any(), any(), any());
    }

    @Test
    void claimInviteRejectsAlreadyLinkedProfile() {
        ProfileInvite invite = invite(Instant.now().plus(1, ChronoUnit.HOURS));
        when(inviteRepository.findByCode(CODE)).thenReturn(Optional.of(invite));
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);
        when(profileRepository.findAllByUserId(10)).thenReturn(List.of(profile()));

        assertThatThrownBy(() -> service.claimInvite(user(), CODE))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.ALREADY_LINKED));
        verify(inviteRepository, never()).claim(any(), any(), any());
    }

    @Test
    void claimInviteReportsRaceOnZeroClaims() {
        ProfileInvite invite = invite(Instant.now().plus(1, ChronoUnit.HOURS));
        when(inviteRepository.findByCode(CODE)).thenReturn(Optional.of(invite));
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);
        when(profileRepository.findAllByUserId(10)).thenReturn(List.of());
        when(inviteRepository.claim(eq(CODE), any(Instant.class), eq(10))).thenReturn(0L);

        assertThatThrownBy(() -> service.claimInvite(user(), CODE))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.INVITE_USED));
        verify(profileRepository, never()).linkUserToProfile(any(), any(), any(), anyBoolean());
    }

    @Test
    void createInviteRejectsNonOwner() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.createInvite(user(), 1))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.NOT_OWNER));
        verify(inviteRepository, never()).save(any());
    }

    @Test
    void createInviteGeneratesSafeCodeAndTtl() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(true);
        when(inviteRepository.save(any(ProfileInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        String code = service.createInvite(user(), 1);

        assertThat(code).hasSize(8);
        assertThat(code.codePoints()).allMatch(ch -> CODE_ALPHABET.indexOf(ch) >= 0);
        ArgumentCaptor<ProfileInvite> captor = ArgumentCaptor.forClass(ProfileInvite.class);
        verify(inviteRepository).save(captor.capture());
        Instant now = Instant.now();
        assertThat(captor.getValue().getProfileId()).isEqualTo(1);
        assertThat(captor.getValue().getExpiresAt())
                .isAfter(now.plus(71, ChronoUnit.HOURS))
                .isBefore(now.plus(73, ChronoUnit.HOURS));
    }

    @Test
    void revokeAccessRejectsNonOwner() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.revokeAccess(user(), 1))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.NOT_OWNER));
        verify(profileRepository, never()).deleteParticipantLinks(any(), any());
    }

    @Test
    void transferOwnershipRejectsNonOwner() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.transferOwnership(user(), 1, 20))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.NOT_OWNER));
    }

    @Test
    void transferOwnershipRejectsSelfTransfer() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(true);

        assertThatThrownBy(() -> service.transferOwnership(user(), 1, 10))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.OWN_PROFILE));
        verify(profileRepository, never()).transferRole(any(), any(), any());
    }

    @Test
    void transferOwnershipRejectsNonMemberTarget() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(true);
        when(profileRepository.isLinkedAs(20, 1, UserRole.MEMBER)).thenReturn(false);

        assertThatThrownBy(() -> service.transferOwnership(user(), 1, 20))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.NOT_FOUND_PARTICIPANT));
        verify(profileRepository, never()).transferRole(any(), any(), any());
    }
}
