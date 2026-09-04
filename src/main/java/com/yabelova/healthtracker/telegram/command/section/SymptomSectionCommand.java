package com.yabelova.healthtracker.telegram.command.section;

import com.yabelova.healthtracker.domain.SymptomLog;
import com.yabelova.healthtracker.domain.SymptomLogProperties;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.service.SymptomService;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.screen.RecordSectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;

@Component
public class SymptomSectionCommand extends AbstractSectionCommand<SymptomLog> {

    private final SymptomService symptomService;

    private static final DateTimeFormatter TIME = new DateTimeFormatterBuilder()
            .appendPattern("dd.MM.yyyy HH:mm")
            .toFormatter();

    public SymptomSectionCommand(ProfileService profileService,
                                 RecordSectionScreen sectionScreen,
                                 ProfileSelectionScreen profileSelectionScreen,
                                 SymptomService symptomService) {
        super(profileService, sectionScreen, profileSelectionScreen);
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
        return BotTexts.SYMPTOM_SECTION_TITLE;
    }

    @Override
    protected List<SymptomLog> listForExport(Long userId, Integer profileId) {
        List<SymptomLog> logs = symptomService.listLastSevenDays(userId, profileId);
        logs.sort(Comparator.comparing(this::effectiveTime));
        return logs;
    }

    @Override
    protected List<SymptomLog> listForDelete(Long userId, Integer profileId) {
        return symptomService.listByProfile(userId, profileId);
    }

    @Override
    protected Integer idOf(SymptomLog record) {
        return record.getId();
    }

    @Override
    protected String deleteLabel(SymptomLog record) {
        return TIME.format(effectiveTime(record)) + " — " + description(record);
    }

    @Override
    protected String formatExport(SymptomLog record) {
        return TIME.format(effectiveTime(record)) + " — " + description(record);
    }

    @Override
    protected String exportEmpty() {
        return BotTexts.SYMPTOM_EXPORT_EMPTY;
    }

    @Override
    protected void delete(Long userId, Integer profileId, Integer id) {
        symptomService.delete(userId, profileId, id);
    }

    /**
     * Если пользователь не задал время симптома — используем время создания записи.
     */
    private LocalDateTime effectiveTime(SymptomLog log) {
        LocalDateTime userTime = properties(log).getSymptomTime();
        return userTime != null
                ? userTime
                : LocalDateTime.ofInstant(log.getCreatedAt(), ZoneId.systemDefault());
    }

    private String description(SymptomLog log) {
        String description = properties(log).getDescription();
        return description == null ? "" : description;
    }

    private SymptomLogProperties properties(SymptomLog log) {
        return log.getProperties() != null ? log.getProperties() : new SymptomLogProperties();
    }
}
