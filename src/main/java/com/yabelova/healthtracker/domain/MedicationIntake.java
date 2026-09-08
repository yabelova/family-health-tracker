package com.yabelova.healthtracker.domain;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("t_medication_intakes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationIntake {

    @Id
    @Nullable
    private Integer id;

    @Column("profile_id")
    private Integer profileId;

    @Column("course_id")
    @Nullable
    private Integer courseId;

    @Column("created_by")
    @Nullable
    private Long createdBy;

    @Column("created_at")
    private Instant createdAt;

    @Nullable
    private MedicationIntakeProperties properties;
}
