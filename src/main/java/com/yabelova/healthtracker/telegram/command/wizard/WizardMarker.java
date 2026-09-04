package com.yabelova.healthtracker.telegram.command.wizard;

import java.util.HashMap;
import java.util.Map;

/**
 * Состояние пошаговой анкеты (визарда), переносимое между шагами через диспетчер.
 * Список шагов {@code fields} восстанавливается рефлексией из {@link #formClass}.
 */
public record WizardMarker(
        Class<?> formClass,
        int currentStep,
        Map<String, Object> answers,
        Integer profileId
) {

    public WizardMarker {
        answers = new HashMap<>(answers);
    }

    public WizardMarker withCurrentStep(int step) {
        return new WizardMarker(formClass, step, answers, profileId);
    }

    public WizardMarker withAnswer(String fieldName, Object value) {
        Map<String, Object> next = new HashMap<>(answers);
        next.put(fieldName, value);
        return new WizardMarker(formClass, currentStep, next, profileId);
    }
}
