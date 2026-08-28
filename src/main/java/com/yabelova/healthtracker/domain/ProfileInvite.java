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

@Table("t_profile_invites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInvite {

    @Id
    @Nullable
    private Integer id;

    @Column("profile_id")
    private Integer profileId;

    private String code;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("used_at")
    @Nullable
    private Instant usedAt;

    @Column("used_by")
    @Nullable
    private Long usedBy;
}
