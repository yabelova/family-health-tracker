package com.yabelova.healthtracker.telegram.command.section;

import com.yabelova.healthtracker.domain.SymptomLog;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.service.SymptomService;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.screen.RecordSectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Dates;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SymptomSectionCommand extends AbstractSectionCommand<SymptomLog> {

    private final SymptomService symptomService;

    public SymptomSectionCommand(ProfileService profileService,
                                 RecordSectionScreen sectionScreen,
                                 ProfileSelectionScreen profileSelectionScreen,
                                 SymptomService symptomService,
                                 ReplySender reply) {
        super(profileService, sectionScreen, profileSelectionScreen, reply);
        this.symptomService = symptomService;
    }

    @Override
    protected CallbackAction openAction() {
        return CallbackAction.SYMPTOM_LOG;
    }

    @Override
    protected CallbackAction addAction() {
        return CallbackAction.SYMPTOM_ADD;
    }

    @Override
    protected CallbackAction exportAction() {
        return CallbackAction.SYMPTOM_EXPORT;
    }

    @Override
    protected CallbackAction deleteAction() {
        return CallbackAction.SYMPTOM_DELETE;
    }

    @Override
    protected CallbackAction deleteSelectedAction() {
        return CallbackAction.SYMPTOM_DELETE_SELECTED;
    }

    @Override
    protected String sectionTitle() {
        return BotTexts.INLINE_BTN_MENU_SYMPTOM;
    }

    @Override
    protected List<SymptomLog> listForExport(Integer userId, Integer profileId) {
        List<SymptomLog> logs = symptomService.listLastSevenDays(userId, profileId);
        logs.sort(Comparator.comparing(log -> log.getProperties().getSymptomTime()));
        return logs;
    }

    @Override
    protected List<SymptomLog> listForDelete(Integer userId, Integer profileId) {
        return symptomService.listByProfile(userId, profileId);
    }

    @Override
    protected Integer idOf(SymptomLog record) {
        return record.getId();
    }

    @Override
    protected String deleteLabel(SymptomLog record) {
        return symptomLine(record);
    }

    @Override
    protected String exportLine(SymptomLog record) {
        return symptomLine(record);
    }

    @Override
    protected String exportEmpty() {
        return BotTexts.SYMPTOM_EXPORT_EMPTY;
    }

    @Override
    protected void delete(Integer userId, Integer profileId, Integer id) {
        symptomService.delete(userId, profileId, id);
    }

    private String symptomLine(SymptomLog record) {
        return Dates.DATE_TIME.format(record.getProperties().getSymptomTime())
                + " — " + description(record);
    }

    private String description(SymptomLog log) {
        String description = log.getProperties().getDescription();
        return description == null ? "" : description;
    }
}
