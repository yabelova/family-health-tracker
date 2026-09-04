package com.yabelova.healthtracker.telegram.command.wizard;

import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.service.MedicationCourseService;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.command.section.MedicationCourseSectionCommand;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import org.springframework.stereotype.Component;

/**
 * Запись курса лечения через универсальную пошаговую анкету. Вся логика шагов в {@link AbstractWizardCommand}
 */
@Component
public class MedicationCourseWizardCommand extends AbstractWizardCommand<MedicationCourseProperties> {

    private final MedicationCourseService medicationCourseService;

    public MedicationCourseWizardCommand(ProfileService profileService,
                                         KeyboardFactory keyboard,
                                         ProfileSelectionScreen profileSelectionScreen,
                                         MedicationCourseService medicationCourseService,
                                         MedicationCourseSectionCommand sectionCommand) {
        super(profileService, keyboard, sectionCommand, profileSelectionScreen);
        this.medicationCourseService = medicationCourseService;
    }

    @Override
    protected Class<MedicationCourseProperties> formClass() {
        return MedicationCourseProperties.class;
    }

    @Override
    protected CallbackAction startCallbackAction() {
        return CallbackAction.MEDICATION_ADD;
    }

    @Override
    protected Object save(MedicationCourseProperties properties, Integer profileId, Long createdBy) {
        return medicationCourseService.save(profileId, createdBy, properties);
    }

    @Override
    protected String savedMessage() {
        return BotTexts.MEDICATION_COURSE_SAVED;
    }

    @Override
    protected String cancelledMessage() {
        return BotTexts.MEDICATION_COURSE_CANCELLED;
    }
}
