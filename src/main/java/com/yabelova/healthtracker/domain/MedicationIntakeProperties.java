package com.yabelova.healthtracker.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationIntakeProperties {

    private String medication;
    private LocalDateTime takenAt;
    private Integer doses;
}
