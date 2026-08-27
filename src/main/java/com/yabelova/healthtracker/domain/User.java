package com.yabelova.healthtracker.domain;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalTime;

@Table("t_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Persistable<Long> {

    @Id
    private Long id;

    @Nullable
    private String username;

    @Column("first_name")
    private String firstName;

    @Transient
    @Nullable
    private Integer activeProfileId;

    @Column("notification_time")
    @Nullable
    private LocalTime notificationTime;

    @Column("created_at")
    @Nullable
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewEntry = false;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public boolean isNew() {
        return this.isNewEntry;
    }
}
