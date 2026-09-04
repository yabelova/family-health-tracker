package com.yabelova.healthtracker.wizard;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет саму разметку анкет: каждый класс, помеченный
 * {@link Wizard}, должен иметь корректно размеченные {@link WizardField}-поля.
 * Новые анкеты и новые поддерживаемые типы подхватываются автоматически — тест
 * не требует доработки при их появлении.
 */
class FormMarkupTest {

    private static final String BASE_PACKAGE = "com.yabelova.healthtracker.domain";

    @Test
    void marksAtLeastOneWizardClass() {
        assertThat(scanWizardClasses()).isNotEmpty();
    }

    @Test
    void eachWizardFieldIsValid() {
        for (Class<?> formClass : scanWizardClasses()) {
            assertMarkup(formClass);
        }
    }

    // ===== Сканирование =====

    private static List<Class<?>> scanWizardClasses() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Wizard.class));

        List<Class<?>> classes = new ArrayList<>();
        for (var metadata : scanner.findCandidateComponents(BASE_PACKAGE)) {
            try {
                classes.add(Class.forName(metadata.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Не удалось загрузить класс " + metadata.getBeanClassName(), e);
            }
        }
        return classes;
    }

    // ===== Проверка разметки одного класса =====

    private static void assertMarkup(Class<?> formClass) {
        List<Field> marked = markedFields(formClass);

        Set<String> labels = new HashSet<>();
        Set<Integer> orders = new HashSet<>();

        String prefix = "Разметка " + formClass.getSimpleName() + ": ";

        for (Field field : marked) {
            WizardField annotation = field.getAnnotation(WizardField.class);

            assertThat(annotation.label().trim())
                    .as(prefix + "поле \"" + field.getName() + "\" — label непустой")
                    .isNotEmpty();

            assertThat(annotation.order())
                    .as(prefix + "поле \"" + field.getName() + "\" — order > 0")
                    .isPositive();

            assertThat(WizardValidator.supports(field.getType()))
                    .as(prefix + "поле \"" + field.getName() + "\" — тип " + field.getType().getSimpleName()
                            + " поддерживается анкетой")
                    .isTrue();

            assertThat(labels.add(annotation.label().trim()))
                    .as(prefix + "дубликат label \"" + annotation.label() + "\"")
                    .isTrue();

            assertThat(orders.add(annotation.order()))
                    .as(prefix + "дубликат order=" + annotation.order() + " (поле \"" + field.getName() + "\")")
                    .isTrue();
        }
    }

    private static List<Field> markedFields(Class<?> formClass) {
        List<Field> marked = new ArrayList<>();
        for (Field field : formClass.getDeclaredFields()) {
            if (field.getAnnotation(WizardField.class) != null) {
                marked.add(field);
            }
        }
        return marked;
    }
}
