package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Уведомление «заканчивается лекарство» при отметке приема: пересечение порога нехватки {@code dosesPerDay} сверху вниз.
 * Повторные отметки в зоне нехватки уведомления не вызывают.
 */
@Service
@RequiredArgsConstructor
public class LowStockAlertBuilder {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    /**
     * Данные для уведомления участников профиля кроме отметившего, или {@code null}, если курс отсутствует
     * или порог не пересечен. Курс передается в состоянии после приема (счетчик принятых доз уже увеличен).
     */
    public LowStockAlertData lowStockCrossingData(Integer profileId, Integer exceptUserId,
                                                  MedicationCourse course, Integer dosesAdded) {
        if (course == null || !course.isStockThresholdCrossedBy(dosesAdded)) {
            return null;
        }
        Profile profile = profileRepository.findById(profileId).orElse(null);
        if (profile == null) {
            return null;
        }

        List<Integer> otherIds = profileRepository.findProfileParticipants(List.of(profileId)).stream()
                .map(ProfileRepository.ProfileParticipant::userId)
                .filter(userId -> !userId.equals(exceptUserId))
                .toList();
        List<User> recipients = userRepository.findAllById(otherIds);
        return new LowStockAlertData(
                profile.getName(),
                course.getProperties().getMedication(),
                course.remainingDoses(),
                recipients);
    }

    public record LowStockAlertData(String profileName, String medication, int remaining, List<User> recipients) {
    }
}
