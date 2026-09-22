package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.domain.UserRole;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.repository.ProfileInviteRepository;
import com.yabelova.healthtracker.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static com.yabelova.healthtracker.exception.ProfileOperationException.Error;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты профиля: создание, активный профиль, переименование и удаление.
 */
class ProfileServiceUnitTest {

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final ProfileInviteRepository inviteRepository = mock(ProfileInviteRepository.class);
    private final ProfileService service = new ProfileService(profileRepository, inviteRepository);

    private static User user() {
        return User.builder().id(10).telegramId(10 * 10L).telegramFirstName("Пользователь").build();
    }

    private static Profile profile() {
        return Profile.builder().id(1).name("Профиль 1").build();
    }

    @Test
    void createProfileSavesLinksOwnerAndActivates() {
        User user = user();
        Profile saved = profile();
        when(profileRepository.save(any(Profile.class))).thenReturn(saved);
        when(profileRepository.isLinked(10, 1)).thenReturn(true);

        Profile result = service.createProfile(user, "Профиль 1");

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Профиль 1");
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        verify(profileRepository).linkUserToProfile(10, 1, UserRole.OWNER, false);
        verify(profileRepository).deactivateAllActive(10);
        verify(profileRepository).activateProfile(10, 1);
        assertThat(user.getActiveProfileId()).isEqualTo(1);
    }

    @Test
    void setActiveProfileRejectsUnlinkedProfile() {
        when(profileRepository.isLinked(10, 1)).thenReturn(false);

        assertThatThrownBy(() -> service.setActiveProfile(user(), 1))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.NOT_FOUND_PARTICIPANT));
        verify(profileRepository, never()).deactivateAllActive(any());
        verify(profileRepository, never()).activateProfile(any(), any());
    }

    @Test
    void setActiveProfileSwitchesActiveProfile() {
        User user = user();
        when(profileRepository.isLinked(10, 1)).thenReturn(true);

        service.setActiveProfile(user, 1);

        verify(profileRepository).deactivateAllActive(10);
        verify(profileRepository).activateProfile(10, 1);
        assertThat(user.getActiveProfileId()).isEqualTo(1);
    }

    @Test
    void renameProfileRejectsNonOwner() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.renameProfile(user(), 1, "Новое"))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.NOT_OWNER));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void renameProfileChangesName() {
        User user = user();
        Profile profile = profile();
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(true);
        when(profileRepository.findById(1)).thenReturn(Optional.of(profile));

        service.renameProfile(user, 1, "Новое");

        assertThat(profile.getName()).isEqualTo("Новое");
        verify(profileRepository).save(profile);
    }

    @Test
    void deleteProfileRejectsNonOwner() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteProfile(user(), 1))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.NOT_OWNER));
    }

    @Test
    void deleteProfileRejectsProfileWithParticipants() {
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(true);
        when(profileRepository.countOtherParticipants(1, 10)).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteProfile(user(), 1))
                .isInstanceOfSatisfying(ProfileOperationException.class,
                        e -> assertThat(e.getError()).isEqualTo(Error.PROFILE_HAS_PARTICIPANTS));
        verify(profileRepository, never()).deleteById(any());
    }

    @Test
    void deleteProfileDeletesAndClearsActiveMark() {
        User user = user();
        user.setActiveProfileId(1);
        when(profileRepository.isLinkedAs(10, 1, UserRole.OWNER)).thenReturn(true);
        when(profileRepository.countOtherParticipants(1, 10)).thenReturn(0L);

        service.deleteProfile(user, 1);

        verify(profileRepository).deleteById(1);
        assertThat(user.getActiveProfileId()).isNull();
    }
}
