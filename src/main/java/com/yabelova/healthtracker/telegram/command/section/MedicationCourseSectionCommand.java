package com.yabelova.healthtracker.telegram.command.section;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.service.MedicationCourseService;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.screen.RecordSectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Dates;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MedicationCourseSectionCommand extends AbstractSectionCommand<MedicationCourse> {

    private final MedicationCourseService courseService;

    public MedicationCourseSectionCommand(ProfileService profileService,
                                          RecordSectionScreen sectionScreen,
                                          ProfileSelectionScreen profileSelectionScreen,
                                          MedicationCourseService courseService,
                                          ReplySender reply) {
        super(profileService, sectionScreen, profileSelectionScreen, reply);
        this.courseService = courseService;
    }

    @Override
    protected CallbackAction openAction() {
        return CallbackAction.MEDICATION_COURSE;
    }

    @Override
    protected CallbackAction addAction() {
        return CallbackAction.MEDICATION_ADD;
    }

    @Override
    protected CallbackAction exportAction() {
        return CallbackAction.MEDICATION_EXPORT;
    }

    @Override
    protected CallbackAction deleteAction() {
        return CallbackAction.MEDICATION_DELETE;
    }

    @Override
    protected CallbackAction deleteSelectedAction() {
        return CallbackAction.MEDICATION_DELETE_SELECTED;
    }

    @Override
    protected String sectionTitle() {
        return BotTexts.MEDICATION_COURSE_SECTION_TITLE;
    }

    @Override
    protected List<MedicationCourse> listForExport(Long userId, Integer profileId) {
        return courseService.listByProfile(userId, profileId);
    }

    @Override
    protected List<MedicationCourse> listForDelete(Long userId, Integer profileId) {
        return courseService.listByProfile(userId, profileId);
    }

    @Override
    protected Integer idOf(MedicationCourse record) {
        return record.getId();
    }

    @Override
    protected String deleteLabel(MedicationCourse record) {
        MedicationCourseProperties properties = record.getProperties();
        StringBuilder sb = new StringBuilder(properties.getMedication());
        if (properties.getDaysCount() != null) {
            sb.append(" ").append(properties.getDaysCount()).append(" дн.");
        }
        if (properties.getStartDate() != null) {
            sb.append(" с ").append(Dates.DATE.format(properties.getStartDate()));
        }
        return sb.toString();
    }

    @Override
    protected String formatExport(MedicationCourse record) {
        MedicationCourseProperties properties = record.getProperties();
        StringBuilder sb = new StringBuilder();
        sb.append("💊 ").append(properties.getMedication());

        java.time.LocalDate startDate = properties.getStartDate();
        if (startDate != null) {
            sb.append(" — с ").append(Dates.DATE.format(startDate));
        }
        if (properties.getDaysCount() != null) {
            sb.append(", ").append(properties.getDaysCount()).append(" дн.");
        }
        if (properties.getDosesPerDay() != null) {
            sb.append(", ").append(properties.getDosesPerDay()).append(" р./день");
        }
        if (record.getRemainingDoses() != null) {
            sb.append(", остаток: ").append(record.getRemainingDoses());
        }
        return sb.toString();
    }

    @Override
    protected String exportEmpty() {
        return BotTexts.MEDICATION_COURSE_EXPORT_EMPTY;
    }

    @Override
    protected void delete(Long userId, Integer profileId, Integer id) {
        courseService.delete(userId, profileId, id);
    }
}
