package com.yabelova.healthtracker.domain;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("t_medication_courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationCourse {

    @Id
    @Nullable
    private Integer id;

    private Integer profileId;

    @Nullable
    private Integer createdBy;

    @Nullable
    private Instant createdAt;

    @Nullable
    private MedicationCourseProperties properties;

    @Nullable
    private Integer dosesTaken;

    /**
     * Остаток доз: вычисляется из упаковки и счетчика принятых доз
     */
    @Nullable
    public Integer remainingDoses() {
        if (properties == null || properties.getDosesPerPackage() == null) {
            return null;
        }
        int taken = dosesTaken == null ? 0 : dosesTaken;
        return Math.max(0, properties.getDosesPerPackage() - taken);
    }

    /**
     * Перерасход: принятых доз больше, чем доз в упаковке
     */
    public boolean isOverrun() {
        if (dosesTaken == null || properties == null || properties.getDosesPerPackage() == null) {
            return false;
        }
        return dosesTaken > properties.getDosesPerPackage();
    }
}
