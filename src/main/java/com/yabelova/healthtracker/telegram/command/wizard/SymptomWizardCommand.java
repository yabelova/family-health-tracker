package com.yabelova.healthtracker.telegram.command.wizard;

import com.yabelova.healthtracker.domain.SymptomLogProperties;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.service.SymptomService;
import com.yabelova.healthtracker.telegram.command.section.SymptomSectionCommand;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.springframework.stereotype.Component;

/**
 * Запись симптома через универсальную пошаговую анкету. Вся логика шагов в {@link AbstractWizardCommand}
 */
@Component
public class SymptomWizardCommand extends AbstractWizardCommand<SymptomLogProperties> {

    private final SymptomService symptomService;

    public SymptomWizardCommand(ProfileService profileService,
                                KeyboardFactory keyboard,
                                ProfileSelectionScreen profileSelectionScreen,
                                SymptomService symptomService,
                                SymptomSectionCommand sectionCommand,
                                ReplySender reply) {
        super(profileService, keyboard, sectionCommand, profileSelectionScreen, reply);
        this.symptomService = symptomService;
    }

    @Override
    protected Class<SymptomLogProperties> formClass() {
        return SymptomLogProperties.class;
    }

    @Override
    protected CallbackAction startCallbackAction() {
        return CallbackAction.SYMPTOM_ADD;
    }

    @Override
    protected Object save(SymptomLogProperties properties, Integer profileId, Long createdBy) {
        return symptomService.save(profileId, createdBy, properties);
    }

    @Override
    protected String savedMessage() {
        return BotTexts.SYMPTOM_SAVED;
    }

    @Override
    protected String cancelledMessage() {
        return BotTexts.SYMPTOM_CANCELLED;
    }
}
