package com.yabelova.healthtracker.wizard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для полей data-класса, которые участвуют в пошаговой анкете (визарде).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface WizardField {

    /**
     * Русское наименование поля для показа юзеру
     */
    String label();

    /**
     * Порядковый номер шага анкеты
     */
    int order();

    /**
     * Является ли поле опциональным
     */
    boolean optional() default false;
}
