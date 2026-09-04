package com.yabelova.healthtracker.wizard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Собирает описание пошаговой анкеты из data-класса с аннотацией {@link Wizard}:
 * перебирает поля с аннотацией {@link WizardField}, и упорядочивает их по {@code order}.
 * Вызывается один раз (в конструкторе команды), результат кешируется.
 */
public final class WizardReflection {

    private WizardReflection() {
    }

    public static List<WizardStep> extractSteps(Class<?> clazz) {
        if (clazz.getAnnotation(Wizard.class) == null) {
            return List.of();
        }

        List<WizardStep> steps = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            WizardField annotation = field.getAnnotation(WizardField.class);
            if (annotation == null || annotation.label().isBlank()) {
                continue;
            }
            if (!WizardValidator.supports(field.getType())) {
                continue;
            }
            steps.add(new WizardStep(
                    annotation.label(),
                    field.getName(),
                    field.getType(),
                    annotation.order(),
                    annotation.optional()
            ));
        }

        steps.sort(Comparator.comparingInt(WizardStep::order));

        return steps;
    }
}
