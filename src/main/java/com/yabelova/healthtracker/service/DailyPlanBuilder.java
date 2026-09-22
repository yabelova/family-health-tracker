package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import com.yabelova.healthtracker.repository.MedicationCourseRepository.MedicationCoursePlanRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Собирает для пользователей план на сегодня по всем профилям.
 */
@Service
@RequiredArgsConstructor
public class DailyPlanBuilder {

    public static final DailyPlan EMPTY = new DailyPlan(List.of());

    private final MedicationCourseRepository courseRepository;

    public DailyPlan build(User user, LocalDate day) {
        return buildAll(List.of(user.getId()), day).getOrDefault(user.getId(), EMPTY);
    }

    /**
     * Планы на сегодня для всех пользователей: {@code userId -> DailyPlan}.
     * Пользователь без строк в выборке (нет профилей или активных курсов) в мапе отсутствует -
     * это «пустой день» для получателя.
     */
    public Map<Integer, DailyPlan> buildAll(Collection<Integer> userIds, LocalDate day) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, UserAccumulator> byUser = new LinkedHashMap<>();
        for (MedicationCoursePlanRow row : courseRepository.findMedicationCoursePlanRowsByUserIds(userIds, day)) {
            UserAccumulator user = byUser.computeIfAbsent(row.userId(), id -> new UserAccumulator());
            user.profile(row.profileId(), row.profileName())
                    .add(new CourseLine(row.medication(), row.dosesPerDay()));
        }
        Map<Integer, DailyPlan> plans = new LinkedHashMap<>();
        byUser.forEach((userId, acc) -> plans.put(userId, acc.toDailyPlan()));
        return plans;
    }

    public record DailyPlan(List<DailyPlanProfile> profiles) {
    }

    public record DailyPlanProfile(String profileName, List<CourseLine> courseLines) {
    }

    public record CourseLine(String medication, Integer dosesPerDay) {
    }

    private record UserAccumulator(Map<Integer, ProfileAccumulator> profilesById) {
        UserAccumulator() {
            this(new LinkedHashMap<>());
        }

        ProfileAccumulator profile(int profileId, String profileName) {
            return profilesById.computeIfAbsent(profileId, id -> new ProfileAccumulator(profileName, new ArrayList<>()));
        }

        DailyPlan toDailyPlan() {
            return new DailyPlan(profilesById.values().stream()
                    .map(ProfileAccumulator::toDailyPlanProfile)
                    .toList());
        }
    }

    private record ProfileAccumulator(String profileName, List<CourseLine> courseLines) {
        void add(CourseLine courseLine) {
            courseLines.add(courseLine);
        }

        DailyPlanProfile toDailyPlanProfile() {
            return new DailyPlanProfile(profileName, List.copyOf(courseLines));
        }
    }
}
