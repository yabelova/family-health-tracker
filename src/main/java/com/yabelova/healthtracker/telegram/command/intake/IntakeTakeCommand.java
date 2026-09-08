package com.yabelova.healthtracker.telegram.command.intake;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.service.IntakeService;
import com.yabelova.healthtracker.service.MedicationCourseService;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.command.BotCommand;
import com.yabelova.healthtracker.telegram.command.section.IntakeSectionCommand;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Dates;
import com.yabelova.healthtracker.util.Numbers;
import com.yabelova.healthtracker.util.TimeZones;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Отметка приема лекарства — кастомный пошаговый флоу (не визард):
 * препарат → дозы → время → подтверждение. Состояние переносится через {@link IntakeFlowMarker}.
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
    private final IntakeSectionCommand sectionCommand;
    private final ReplySender reply;

    public IntakeTakeCommand(ProfileService profileService,
                             KeyboardFactory keyboard,
                             ProfileSelectionScreen profileSelectionScreen,
                             IntakeService intakeService,
                             MedicationCourseService courseService,
                             IntakeSectionCommand sectionCommand,
                             ReplySender reply) {
        this.profileService = profileService;
        this.keyboard = keyboard;
        this.profileSelectionScreen = profileSelectionScreen;
        this.intakeService = intakeService;
        this.courseService = courseService;
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
            reply.answerCallbackQuery(update.getCallbackQuery().getId());

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
                case WIZARD_CANCEL -> cancel(user, flow);
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

    // ===== Запуск и шаги =====

    private Object start(User user) {
        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
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
            case INTAKE_TAKEN_AT -> advance(user, flow.withTakenAt(LocalDateTime.now(TimeZones.DEFAULT)));
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
        MedicationCourse course = courseService.findActiveForProfileById(user.getId(), flow.profileId(), courseId);
        if (course == null) {
            return reprompt(user, flow, BotTexts.INTAKE_COURSE_NOT_FOUND);
        }
        return advance(user, flow.withCourse(course.getId(), medicationName(course)));
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
        intakeService.save(flow.profileId(), user.getId(), flow.courseId(), properties);
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.INTAKE_SAVED)
                .build());
        sectionCommand.showSection(user);
        return null;
    }

    private Object retry(User user, IntakeFlowMarker flow) {
        IntakeFlowMarker reset = new IntakeFlowMarker(STEP_MEDICATION, null, null, null, null, flow.profileId());
        promptStep(user, reset);
        return reset;
    }

    private Object cancel(User user, IntakeFlowMarker flow) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.INTAKE_CANCELLED)
                .build());
        sectionCommand.showSection(user);
        return null;
    }

    private Object reprompt(User user, IntakeFlowMarker flow, String errorText) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(errorText)
                .build());
        promptStep(user, flow);
        return flow;
    }

    private Object sendError(User user, RecordOperationException e) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(e.getMessage())
                .build());
        profileSelectionScreen.render(user);
        return null;
    }

    // ===== Отображение =====

    private void promptStep(User user, IntakeFlowMarker flow) {
        switch (flow.step()) {
            case STEP_MEDICATION -> promptMedication(user, flow);
            case STEP_DOSES -> promptDoses(user);
            case STEP_TAKEN_AT -> promptTime(user);
            default -> showSummary(user, flow);
        }
    }

    private void promptMedication(User user, IntakeFlowMarker flow) {
        List<MedicationCourse> courses = courseService.listActiveByProfile(user.getId(), flow.profileId());
        String text = courses.isEmpty()
                ? BotTexts.INTAKE_STEP_MEDICATION_NO_COURSES
                : BotTexts.INTAKE_STEP_MEDICATION_PROMPT;
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboard.intakeMedication(courses))
                .build());
    }

    private void promptDoses(User user) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.INTAKE_STEP_DOSES_PROMPT)
                .parseMode("HTML")
                .replyMarkup(keyboard.intakeDoses())
                .build());
    }

    private void promptTime(User user) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.INTAKE_STEP_TAKEN_AT_PROMPT)
                .parseMode("HTML")
                .replyMarkup(keyboard.intakeTime())
                .build());
    }

    private void showSummary(User user, IntakeFlowMarker flow) {
        String body = "• Препарат: " + HtmlUtils.escape(flow.medication())
                + "\n• Дозы: " + flow.doses()
                + "\n• Время: " + HtmlUtils.escape(Dates.DATE_TIME.format(flow.takenAt()));
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.INTAKE_CONFIRM_TEXT.formatted(body))
                .parseMode("HTML")
                .replyMarkup(keyboard.formConfirmKeyboard())
                .build());
    }

    // ===== Помощники =====

    private String medicationName(MedicationCourse course) {
        return course.getProperties().getMedication();
    }
}
