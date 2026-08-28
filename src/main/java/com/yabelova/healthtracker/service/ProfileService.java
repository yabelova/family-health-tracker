package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.ProfileInvite;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.repository.ProfileInviteRepository;
import com.yabelova.healthtracker.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.yabelova.healthtracker.domain.UserRole.MEMBER;
import static com.yabelova.healthtracker.domain.UserRole.OWNER;
import static com.yabelova.healthtracker.exception.ProfileOperationException.Error;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int INVITE_TTL_HOURS = 72;

    private final SecureRandom secureRandom = new SecureRandom();

    private final ProfileRepository profileRepository;
    private final ProfileInviteRepository profileInviteRepository;

    @Transactional
    public Profile createProfile(User user, String name) {
        Profile saved = profileRepository.save(Profile.builder()
                .name(name)
                .createdAt(Instant.now())
                .build());
        profileRepository.linkUserToProfile(user.getId(), saved.getId(), OWNER, false);
        setActiveProfile(user, saved.getId());
        return saved;
    }

    @Transactional
    public void setActiveProfile(User user, Integer profileId) {
        profileRepository.deactivateActive(user.getId());
        profileRepository.activateProfile(user.getId(), profileId);
        user.setActiveProfileId(profileId);
    }

    public List<Profile> getProfiles(User user) {
        return profileRepository.findAllByUserId(user.getId());
    }

    public Profile getActiveProfile(User user) {
        Integer profileId = user.getActiveProfileId();
        if (profileId == null) {
            return null;
        }
        return profileRepository.findById(profileId).orElse(null);
    }

    public boolean isOwner(User user, Integer profileId) {
        return profileRepository.isLinkedAs(user.getId(), profileId, OWNER);
    }

    @Transactional
    public String createInvite(User user, Integer profileId) {
        checkOwner(user, profileId);

        ProfileInvite invite = ProfileInvite.builder()
                .profileId(profileId)
                .code(generateCode())
                .expiresAt(Instant.now().plus(INVITE_TTL_HOURS, ChronoUnit.HOURS))
                .build();
        profileInviteRepository.save(invite);
        return invite.getCode();
    }

    @Transactional
    public Profile claimInvite(User user, String code) {
        String normalized = code.trim().toUpperCase();

        ProfileInvite invite = profileInviteRepository.findByCode(normalized)
                .orElseThrow(() -> new ProfileOperationException(Error.INVITE_NOT_FOUND));

        if (invite.getUsedAt() != null) {
            throw new ProfileOperationException(Error.INVITE_USED);
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ProfileOperationException(Error.INVITE_EXPIRED);
        }
        if (isOwner(user, invite.getProfileId())) {
            throw new ProfileOperationException(Error.OWN_PROFILE);
        }
        boolean alreadyLinked = profileRepository.findAllByUserId(user.getId()).stream()
                .anyMatch(p -> p.getId().equals(invite.getProfileId()));
        if (alreadyLinked) {
            throw new ProfileOperationException(Error.ALREADY_LINKED);
        }

        long claimed = profileInviteRepository.claim(normalized, Instant.now(), user.getId());
        if (claimed == 0) {
            throw new ProfileOperationException(Error.INVITE_USED);
        }

        profileRepository.linkUserToProfile(user.getId(), invite.getProfileId(), MEMBER, false);
        return profileRepository.findById(invite.getProfileId())
                .orElseThrow(() -> new IllegalStateException("Профиль не найден: id=" + invite.getProfileId()));
    }

    @Transactional
    public void revokeAccess(User user, Integer profileId) {
        checkOwner(user, profileId);
        profileRepository.deleteLinksExceptUser(profileId, user.getId());
        profileInviteRepository.deleteUnusedByProfile(profileId);
    }

    @Transactional
    public void deleteProfile(User user, Integer profileId) {
        checkOwner(user, profileId);
        profileRepository.deleteById(profileId);
        if (profileId.equals(user.getActiveProfileId())) {
            user.setActiveProfileId(null);
        }
    }

    @Transactional
    public void renameProfile(User user, Integer profileId, String name) {
        checkOwner(user, profileId);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalStateException("Профиль не найден: id=" + profileId));
        profile.setName(name);
        profileRepository.save(profile);
    }

    private void checkOwner(User user, Integer profileId) {
        if (!isOwner(user, profileId)) {
            throw new ProfileOperationException(Error.NOT_OWNER);
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}