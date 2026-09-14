package com.yabelova.healthtracker.domain;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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

    private Integer profileId;

    private String code;

    private Instant expiresAt;

    @Nullable
    private Instant usedAt;

    @Nullable
    private Integer usedBy;
}
