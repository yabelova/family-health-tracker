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
    private Integer remainingDoses;
}
