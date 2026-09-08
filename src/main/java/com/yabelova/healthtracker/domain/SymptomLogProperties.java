package com.yabelova.healthtracker.domain;

import com.yabelova.healthtracker.wizard.Wizard;
import com.yabelova.healthtracker.wizard.WizardField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Wizard
public class SymptomLogProperties {

    @WizardField(label = "Описание симптома", order = 1)
    private String description;

    @WizardField(label = "Дата и время симптома", order = 2)
    private LocalDateTime symptomTime;
}
