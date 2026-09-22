package com.yabelova.healthtracker.service;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты уведомления о нехватке лекарства: пересечение порога доз и получатели.
 */
class LowStockAlertBuilderTest {

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LowStockAlertBuilder builder = new LowStockAlertBuilder(profileRepository, userRepository);

    @Test
    void noPackageHasNoLowStock() {
        MedicationCourse course = course(1, 2, null, 3);

        assertThat(builder.lowStockCrossingData(1, 10, course, 1)).isNull();
    }

    @Test
    void remainingAboveThresholdHasNoLowStock() {
        MedicationCourse course = course(1, 2, 10, 2);

        assertThat(builder.lowStockCrossingData(1, 10, course, 1)).isNull();
    }

    @Test
    void alreadyInLowZoneNotCrossed() {
        MedicationCourse course = course(1, 2, 10, 9);

        assertThat(builder.lowStockCrossingData(1, 10, course, 1)).isNull();
    }

    @Test
    void overrunStaysSilentOnRepeatIntake() {
        MedicationCourse course = course(1, 2, 10, 11);

        assertThat(builder.lowStockCrossingData(1, 10, course, 1)).isNull();
    }

    @Test
    void crossingThresholdReturnsRecipientsExceptMarker() {
        when(profileRepository.findById(1)).thenReturn(Optional.of(new Profile(1, "Аня", null)));
        when(profileRepository.findProfileParticipants(List.of(1)))
                .thenReturn(List.of(
                        new ProfileRepository.ProfileParticipant(1, 10, "Папа", null),
                        new ProfileRepository.ProfileParticipant(1, 20, "Мама", null)));
        User mother = User.builder().id(20).telegramId(200L).telegramFirstName("Мама").build();
        when(userRepository.findAllById(List.of(20))).thenReturn(List.of(mother));

        MedicationCourse course = course(1, 2, 10, 8);

        LowStockAlertBuilder.LowStockAlertData data = builder.lowStockCrossingData(1, 10, course, 1);
        assertThat(data).isNotNull();
        assertThat(data.profileName()).isEqualTo("Аня");
        assertThat(data.medication()).isEqualTo("Антибиотик");
        assertThat(data.remaining()).isEqualTo(2);
        assertThat(data.recipients()).containsExactly(mother);
    }

    @Test
    void dosesPerDayNullUsesOneAsThreshold() {
        when(profileRepository.findById(1)).thenReturn(Optional.of(new Profile(1, "Аня", null)));
        when(profileRepository.findProfileParticipants(List.of(1)))
                .thenReturn(List.of(new ProfileRepository.ProfileParticipant(1, 20, "Мама", null)));
        User mother = User.builder().id(20).telegramId(200L).telegramFirstName("Мама").build();
        when(userRepository.findAllById(List.of(20))).thenReturn(List.of(mother));

        MedicationCourse course = course(1, null, 3, 2);

        assertThat(builder.lowStockCrossingData(1, 10, course, 1)).isNotNull();
    }

    @Test
    void crossingThresholdWithNoOtherRecipientsReturnsEmptyData() {
        when(profileRepository.findById(1)).thenReturn(Optional.of(new Profile(1, "Аня", null)));
        when(profileRepository.findProfileParticipants(List.of(1)))
                .thenReturn(List.of(new ProfileRepository.ProfileParticipant(1, 10, "Папа", null)));

        MedicationCourse course = course(1, 2, 10, 8);

        LowStockAlertBuilder.LowStockAlertData data = builder.lowStockCrossingData(1, 10, course, 1);
        assertThat(data).isNotNull();
        assertThat(data.recipients()).isEmpty();
    }

    private MedicationCourse course(Integer profileId, Integer dosesPerDay, Integer dosesPerPackage,
                                    Integer dosesTaken) {
        MedicationCourseProperties properties = new MedicationCourseProperties();
        properties.setMedication("Антибиотик");
        properties.setDosesPerDay(dosesPerDay);
        properties.setDosesPerPackage(dosesPerPackage);
        return MedicationCourse.builder()
                .profileId(profileId)
                .properties(properties)
                .dosesTaken(dosesTaken)
                .build();
    }
}
