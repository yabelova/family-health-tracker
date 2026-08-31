package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.domain.UserRole;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.ProfileRepository.ProfileParticipant;
import com.yabelova.healthtracker.repository.ProfileRepository.ProfileWithRole;
import com.yabelova.healthtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Оркестратор приватности: классификация профилей пользователя для удаления аккаунта
 * и атомарное применение плана удаления
 */
@Service
@RequiredArgsConstructor
public class DataPrivacyService {

    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    /**
     * Решение по общему профилю при удалении аккаунта
     */
    public enum Decision {TRANSFER, REVOKE}

    /**
     * Группы профилей пользователя при расчёте плана удаления:
     * личные (удаляются), где участник (снимается связь) и общие-owner (нужно решение)
     */
    public record ProfileGroups(List<Profile> personal, List<Profile> member, List<OwnerShared> ownerShared) {
    }

    /**
     * Профиль, созданный пользователем и разделяемый с другими участниками
     */
    public record OwnerShared(Profile profile, List<ProfileParticipant> participants) {
    }

    /**
     * Решение юзера по общему профилю: передать владение другому участнику
     * либо отозвать доступ
     */
    public record PrivacyDecision(Integer profileId, Decision decision, Long targetUserId) {
    }

    public ProfileGroups getAndGroupProfiles(User user) {
        List<ProfileWithRole> profilesWithRole = profileRepository.findProfileWithRolesByUserId(user.getId());

        List<Integer> ownedProfileIds = profilesWithRole.stream()
                .filter(r -> r.role() == UserRole.OWNER)
                .map(ProfileWithRole::profileId)
                .toList();

        Map<Integer, List<ProfileParticipant>> participantsMap = Map.of();
        if (!ownedProfileIds.isEmpty()) {
            participantsMap = profileRepository.findProfileParticipants(ownedProfileIds)
                    .stream()
                    .filter(p -> !p.userId().equals(user.getId()))
                    .collect(Collectors.groupingBy(ProfileParticipant::profileId));
        }

        List<Profile> personal = new ArrayList<>();
        List<Profile> member = new ArrayList<>();
        List<OwnerShared> ownerShared = new ArrayList<>();

        for (ProfileWithRole row : profilesWithRole) {
            Profile profile = Profile.builder()
                    .id(row.profileId())
                    .name(row.name())
                    .build();

            if (row.role() == UserRole.OWNER) {
                List<ProfileParticipant> participants =
                        participantsMap.getOrDefault(row.profileId(), List.of());
                if (participants.isEmpty()) {
                    personal.add(profile);
                } else {
                    ownerShared.add(new OwnerShared(profile, participants));
                }
            } else {
                member.add(profile);
            }
        }
        return new ProfileGroups(personal, member, ownerShared);
    }

    /**
     * Применяет план одним транзакционным шагом.
     * Сначала выполняются решения (передачи/отзывы), затем все ставшие личными
     * профили удаляются каскадом, оставшиеся member-связи снимаются, после чего
     * удаляется сам пользователь. Любая ошибка откатывает всё изменение.
     */
    @Transactional
    public void applyDeletionPlan(User user, List<PrivacyDecision> decisions) {
        for (PrivacyDecision decision : decisions) {
            switch (decision.decision()) {
                case TRANSFER -> profileService.transferOwnership(
                        user, decision.profileId(), decision.targetUserId());
                case REVOKE -> profileService.revokeAccess(user, decision.profileId());
            }
        }

        ProfileGroups profiles = getAndGroupProfiles(user);
        for (Profile profile : profiles.personal()) {
            profileService.deleteProfile(user, profile.getId());
        }

        userRepository.deleteById(user.getId());
    }
}
