package com.yabelova.healthtracker.domain;

import com.yabelova.healthtracker.wizard.Wizard;
import com.yabelova.healthtracker.wizard.WizardField;
import lombok.Data;

import java.time.LocalDate;

@Data
@Wizard
public class MedicationCourseProperties {

    @WizardField(label = "Название лекарства", order = 1)
    private String medication;

    @WizardField(label = "Дата начала приема", order = 2)
    private LocalDate startDate;

    @WizardField(label = "Количество дней", order = 3, optional = true)
    private Integer daysCount;

    @WizardField(label = "Количество приемов в день", order = 4)
    private Integer dosesPerDay;

    @WizardField(label = "Количество доз в упаковке", order = 5, optional = true)
    private Integer dosesPerPackage;
}
