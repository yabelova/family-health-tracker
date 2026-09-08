package com.yabelova.healthtracker.telegram.command.section;

import com.yabelova.healthtracker.domain.MedicationIntake;
import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.IntakeService;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.screen.RecordSectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Dates;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IntakeSectionCommand extends AbstractSectionCommand<MedicationIntake> {

    private final IntakeService intakeService;

    public IntakeSectionCommand(ProfileService profileService,
                                RecordSectionScreen sectionScreen,
                                ProfileSelectionScreen profileSelectionScreen,
                                IntakeService intakeService,
                                ReplySender reply) {
        super(profileService, sectionScreen, profileSelectionScreen, reply);
        this.intakeService = intakeService;
    }

    @Override
    protected CallbackAction openAction() {
        return CallbackAction.INTAKE_LOG;
    }

    @Override
    protected CallbackAction addAction() {
        return CallbackAction.INTAKE_ADD;
    }

    @Override
    protected CallbackAction exportAction() {
        return CallbackAction.INTAKE_EXPORT;
    }

    @Override
    protected CallbackAction deleteAction() {
        return CallbackAction.INTAKE_DELETE;
    }

    @Override
    protected CallbackAction deleteSelectedAction() {
        return CallbackAction.INTAKE_DELETE_SELECTED;
    }

    @Override
    protected String addLabel() {
        return BotTexts.INLINE_BTN_INTAKE_ADD;
    }

    @Override
    protected String sectionTitle() {
        return BotTexts.INTAKE_SECTION_TITLE;
    }

    @Override
    protected List<MedicationIntake> listForExport(Long userId, Integer profileId) {
        List<MedicationIntake> intakes = intakeService.listLastSevenDays(userId, profileId);
        intakes.sort(Comparator.comparing(intake -> intake.getProperties().getTakenAt()));
        return intakes;
    }

    @Override
    protected List<MedicationIntake> listForDelete(Long userId, Integer profileId) {
        return intakeService.listByProfile(userId, profileId);
    }

    @Override
    protected Integer idOf(MedicationIntake record) {
        return record.getId();
    }

    @Override
    protected String deleteLabel(MedicationIntake record) {
        return formatExport(record);
    }

    @Override
    protected String formatExport(MedicationIntake record) {
        var properties = record.getProperties();
        return Dates.DATE_TIME.format(properties.getTakenAt())
                + " — 💊 " + properties.getMedication() + ", " + properties.getDoses() + " доз.";
    }

    @Override
    protected String exportEmpty() {
        return BotTexts.INTAKE_EXPORT_EMPTY;
    }

    @Override
    protected void delete(Long userId, Integer profileId, Integer id) {
        intakeService.delete(userId, profileId, id);
    }

    @Override
    protected String sectionPreview(User user, Profile profile) {
        List<MedicationIntake> today = intakeService.listToday(user.getId(), profile.getId());
        today.sort(Comparator.comparing(intake -> intake.getProperties().getTakenAt()));
        if (today.isEmpty()) {
            return "";
        }
        String lines = today.stream()
                .map(intake -> {
                    var properties = intake.getProperties();
                    return BotTexts.INTAKE_TODAY_LINE.formatted(
                            properties.getMedication(),
                            Dates.TIME.format(properties.getTakenAt()),
                            properties.getDoses());
                })
                .collect(Collectors.joining("\n"));
        return BotTexts.INTAKE_TODAY_HEAD.formatted(lines);
    }
}
