package com.yabelova.healthtracker.telegram.command.intake;

import java.time.LocalDateTime;

/**
 * Состояние флоу отметки приема, переносимое между шагами через диспетчер.
 * Шаги: 0 — выбор препарата, 1 — количество доз, 2 — дата-время, 3 — подтверждение.
 */
public record IntakeFlowMarker(
        int step,
        Integer courseId,
        String medication,
        Integer doses,
        LocalDateTime takenAt,
        Integer profileId
) {

    public IntakeFlowMarker withStep(int step) {
        return new IntakeFlowMarker(step, courseId, medication, doses, takenAt, profileId);
    }

    public IntakeFlowMarker withCourse(Integer courseId, String medication) {
        return new IntakeFlowMarker(step, courseId, medication, doses, takenAt, profileId);
    }

    public IntakeFlowMarker withDoses(Integer doses) {
        return new IntakeFlowMarker(step, courseId, medication, doses, takenAt, profileId);
    }

    public IntakeFlowMarker withTakenAt(LocalDateTime takenAt) {
        return new IntakeFlowMarker(step, courseId, medication, doses, takenAt, profileId);
    }
}
