package com.yabelova.healthtracker.wizard;

/**
 * Описание одного шага анкеты (одного поля data-класса с аннотацией {@link WizardField}).
 */
public record WizardStep(
        String label,
        String fieldName,
        Class<?> type,
        int order,
        boolean optional
) {
}
