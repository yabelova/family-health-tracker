package com.yabelova.healthtracker.domain;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Table("t_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Nullable
    private Integer id;

    private Long telegramId;

    @Nullable
    private String telegramUsername;

    private String telegramFirstName;

    @Transient
    @Nullable
    private Integer activeProfileId;

    @Nullable
    private LocalTime notificationTime;

    /**
     * Дата последней отправки ежедневного напоминания.
     */
    @Nullable
    private LocalDate lastNotifiedDate;

    @Nullable
    private Instant createdAt;
}
