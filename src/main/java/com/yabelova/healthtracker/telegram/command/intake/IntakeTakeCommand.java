package com.yabelova.healthtracker.telegram.command.intake;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.service.IntakeService;
import com.yabelova.healthtracker.service.IntakeService.IntakeSaveResult;
import com.yabelova.healthtracker.service.LowStockAlertBuilder;
import com.yabelova.healthtracker.service.MedicationCourseService;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.command.BotCommand;
import com.yabelova.healthtracker.telegram.command.section.IntakeSectionCommand;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Dates;
import com.yabelova.healthtracker.util.Numbers;
import com.yabelova.healthtracker.util.TimeZones;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Отметка приема лекарства — кастомный пошаговый флоу (не визард):
 * препарат - дозы - время - подтверждение. Состояние переносится через {@link IntakeFlowMarker}.
 */
@Component
public class IntakeTakeCommand implements BotCommand {

    private static final int STEP_MEDICATION = 0;
    private static final int STEP_DOSES = 1;
    private static final int STEP_TAKEN_AT = 2;
    private static final int STEP_CONFIRM = 3;

    private final ProfileService profileService;
    private final KeyboardFactory keyboard;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final IntakeService intakeService;
    private final MedicationCourseService courseService;
    private final LowStockAlertBuilder lowStockAlertBuilder;
    private final IntakeSectionCommand sectionCommand;
    private final ReplySender reply;

    public IntakeTakeCommand(ProfileService profileService,
                             KeyboardFactory keyboard,
                             ProfileSelectionScreen profileSelectionScreen,
                             IntakeService intakeService,
                             MedicationCourseService courseService,
                             LowStockAlertBuilder lowStockAlertBuilder,
                             IntakeSectionCommand sectionCommand,
                             ReplySender reply) {
        this.profileService = profileService;
        this.keyboard = keyboard;
        this.profileSelectionScreen = profileSelectionScreen;
        this.intakeService = intakeService;
        this.courseService = courseService;
        this.lowStockAlertBuilder = lowStockAlertBuilder;
        this.sectionCommand = sectionCommand;
        this.reply = reply;
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(
                CallbackAction.INTAKE_ADD,
                CallbackAction.INTAKE_COURSE,
                CallbackAction.INTAKE_DOSES,
                CallbackAction.INTAKE_TAKEN_AT
        );
    }

    @Override
    public Object handleCallback(Update update, User user, Object marker) {
        try {
            CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());

            if (action == CallbackAction.INTAKE_ADD) {
                return start(user);
            }

            if (!(marker instanceof IntakeFlowMarker flow)) {
                return null;
            }

            return switch (action) {
                case INTAKE_COURSE, INTAKE_DOSES, INTAKE_TAKEN_AT -> applyButton(update, user, flow, action);
                case WIZARD_SKIP -> skip(user, flow);
                case WIZARD_CONFIRM -> confirm(user, flow);
                case WIZARD_RETRY -> retry(user, flow);
                case WIZARD_CANCEL -> cancel(user);
                default -> null;
            };
        } catch (RecordOperationException e) {
            return sendError(user, e);
        }
    }

    @Override
    public Object handlePendingText(Update update, User user, Object marker) {
        try {
            if (!(marker instanceof IntakeFlowMarker flow)) {
                return null;
            }
            return applyText(user, flow, update.getMessage().getText().trim());
        } catch (RecordOperationException e) {
            return sendError(user, e);
        }
    }

    private Object start(User user) {
        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            return null;
        }
        IntakeFlowMarker flow = new IntakeFlowMarker(STEP_MEDICATION, null, null, null, null, profile.getId());
        promptStep(user, flow);
        return flow;
    }

    private Object applyButton(Update update, User user, IntakeFlowMarker flow, CallbackAction action) {
        String payload = CallbackAction.payloadOf(update.getCallbackQuery().getData());
        return switch (action) {
            case INTAKE_COURSE -> selectCourse(user, flow, payload);
            case INTAKE_DOSES -> applyDoses(user, flow, payload);
            case INTAKE_TAKEN_AT ->
                    advance(user, flow.withTakenAt(LocalDateTime.now(TimeZones.DEFAULT).truncatedTo(ChronoUnit.MINUTES)));
            default -> null;
        };
    }

    private Object applyText(User user, IntakeFlowMarker flow, String raw) {
        return switch (flow.step()) {
            case STEP_MEDICATION -> raw.isEmpty()
                    ? reprompt(user, flow, BotTexts.INTAKE_STEP_MEDICATION_INVALID)
                    : advance(user, flow.withCourse(null, raw));
            case STEP_DOSES -> applyDoses(user, flow, raw);
            case STEP_TAKEN_AT -> {
                LocalDateTime takenAt = Dates.parseLocalDateTime(raw);
                if (takenAt == null) {
                    yield reprompt(user, flow, BotTexts.INTAKE_INVALID_INPUT);
                }
                yield advance(user, flow.withTakenAt(takenAt));
            }
            default -> {
                showSummary(user, flow);
                yield flow;
            }
        };
    }

    private Object selectCourse(User user, IntakeFlowMarker flow, String payload) {
        Integer courseId = Numbers.parseInt(payload);
        MedicationCourse course = courseService.findForIntakeName(courseId);
        if (course == null) {
            return reprompt(user, flow, BotTexts.INTAKE_COURSE_NOT_FOUND);
        }
        return advance(user, flow.withCourse(course.getId(), course.getProperties().getMedication()));
    }

    private Object applyDoses(User user, IntakeFlowMarker flow, String raw) {
        Integer doses = Numbers.parseInt(raw);
        if (doses == null || doses < 1) {
            return reprompt(user, flow, BotTexts.INTAKE_INVALID_INPUT);
        }
        return advance(user, flow.withDoses(doses));
    }

    private Object skip(User user, IntakeFlowMarker flow) {
        if (flow.step() == STEP_DOSES) {
            return advance(user, flow.withDoses(1));
        }
        return flow;
    }

    private Object advance(User user, IntakeFlowMarker flow) {
        IntakeFlowMarker next = flow.withStep(flow.step() + 1);
        if (next.step() == STEP_CONFIRM) {
            showSummary(user, next);
        } else {
            promptStep(user, next);
        }
        return next;
    }

    private Object confirm(User user, IntakeFlowMarker flow) {
        MedicationIntakeProperties properties = new MedicationIntakeProperties(
                flow.medication(),
                flow.takenAt(),
                flow.doses());
        try {
            IntakeSaveResult result = intakeService.save(flow.profileId(), user.getId(), flow.courseId(), properties);
            reply.send(user, savedText(result.course()));
            sendLowStockNotification(user, flow, result.course());
        } catch (DuplicateKeyException e) {
            reply.send(user, BotTexts.INTAKE_DUPLICATE);
        }
        sectionCommand.showSection(user);
        return null;
    }

    private String savedText(MedicationCourse course) {
        if (course == null) {
            return BotTexts.INTAKE_SAVED;
        }
        Integer remaining = course.remainingDoses();
        if (remaining == null) {
            return BotTexts.INTAKE_SAVED;
        }
        if (course.isOverrun()) {
            return BotTexts.INTAKE_SAVED + "\n" + BotTexts.INTAKE_REMAINING_WARNING.formatted(0);
        }
        return BotTexts.INTAKE_SAVED + "\n" + BotTexts.INTAKE_REMAINING.formatted(remaining);
    }

    private void sendLowStockNotification(User user, IntakeFlowMarker flow, MedicationCourse course) {
        LowStockAlertBuilder.LowStockAlertData data = lowStockAlertBuilder.lowStockCrossingData(
                flow.profileId(), user.getId(), course, flow.doses());
        if (data == null) {
            return;
        }
        String text = BotTexts.NOTIFICATION_LOW_STOCK.formatted(
                HtmlUtils.bold(data.profileName()),
                HtmlUtils.escape(data.medication()),
                data.remaining());
        for (User recipient : data.recipients()) {
            reply.send(recipient, text, ParseMode.HTML);
        }
    }

    private Object retry(User user, IntakeFlowMarker flow) {
        IntakeFlowMarker reset = new IntakeFlowMarker(STEP_MEDICATION, null, null, null, null, flow.profileId());
        promptStep(user, reset);
        return reset;
    }

    private Object cancel(User user) {
        reply.send(user, BotTexts.INTAKE_CANCELLED);
        sectionCommand.showSection(user);
        return null;
    }

    private Object reprompt(User user, IntakeFlowMarker flow, String errorText) {
        reply.send(user, errorText);
        promptStep(user, flow);
        return flow;
    }

    private Object sendError(User user, RecordOperationException e) {
        reply.send(user, e.getMessage());
        profileSelectionScreen.render(user);
        return null;
    }

    private void promptStep(User user, IntakeFlowMarker flow) {
        switch (flow.step()) {
            case STEP_MEDICATION -> promptMedication(user, flow);
            case STEP_DOSES -> promptDoses(user);
            case STEP_TAKEN_AT -> promptTime(user);
            default -> showSummary(user, flow);
        }
    }

    private void promptMedication(User user, IntakeFlowMarker flow) {
        List<MedicationCourse> courses = courseService.listIntakeOptions(
                user.getId(), flow.profileId(), LocalDate.now(TimeZones.DEFAULT));
        String text = courses.isEmpty()
                ? BotTexts.INTAKE_STEP_MEDICATION_NO_COURSES
                : BotTexts.INTAKE_STEP_MEDICATION_PROMPT;
        reply.send(user, text, ParseMode.HTML,
                keyboard.intakeMedication(courses));
    }

    private void promptDoses(User user) {
        reply.send(user, BotTexts.INTAKE_STEP_DOSES_PROMPT, ParseMode.HTML,
                keyboard.intakeDoses());
    }

    private void promptTime(User user) {
        reply.send(user, BotTexts.INTAKE_STEP_TAKEN_AT_PROMPT, ParseMode.HTML,
                keyboard.intakeTime());
    }

    private void showSummary(User user, IntakeFlowMarker flow) {
        String body = "• Препарат: " + HtmlUtils.escape(flow.medication())
                + "\n• Дозы: " + flow.doses()
                + "\n• Время: " + HtmlUtils.escape(Dates.DATE_TIME.format(flow.takenAt()));
        reply.send(user, BotTexts.INTAKE_CONFIRM_TEXT.formatted(body), ParseMode.HTML,
                keyboard.formConfirmKeyboard());
    }
}
