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
import java.time.LocalTime;

@Table("t_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private Long id;

    @Nullable
    private String username;

    @Column("first_name")
    private String firstName;

    @Column("current_state")
    private UserState currentState;

    @Column("selected_subject_id")
    @Nullable
    private Integer selectedSubjectId;

    @Column("notification_time")
    @Nullable
    private LocalTime notificationTime;

    @Column("created_at")
    @Nullable
    private Instant createdAt;
}
