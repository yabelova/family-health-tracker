package com.yabelova.healthtracker.wizard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для data-класса анкеты (визарда): помечает класс, поля которого размечены {@link WizardField}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Wizard {
}
