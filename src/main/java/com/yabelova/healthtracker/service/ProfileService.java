package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.yabelova.healthtracker.domain.UserRole.OWNER;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

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
}