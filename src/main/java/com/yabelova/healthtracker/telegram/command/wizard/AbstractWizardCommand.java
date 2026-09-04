package com.yabelova.healthtracker.telegram.command.wizard;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.RecordOperationException;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.command.BotCommand;
import com.yabelova.healthtracker.telegram.command.section.AbstractSectionCommand;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.wizard.WizardReflection;
import com.yabelova.healthtracker.wizard.WizardStep;
import com.yabelova.healthtracker.wizard.WizardValidator;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Общий каркас пошаговой анкеты (визарда). Реализует весь флоу: список шагов
 * строится рефлексией из {@link #formClass()} один раз, обработка текста и
 * универсальных wizard-кнопок, подтверждение/отмена, экран итогов.
 * <p>
 * Конкретная анкета наследует этот класс и задаёт только: класс полей, callback
 * действия запуска, сохранение результата и тексты об успехе/отмене. Никакой
 * логики шагов в подклассе нет.
 *
 * @param <P> класс со свойствами анкеты (поля, размеченные {@code @WizardField})
 */
public abstract class AbstractWizardCommand<P> implements BotCommand {

    private final ProfileService profileService;
    private final KeyboardFactory keyboard;
    private final AbstractSectionCommand<?> sectionCommand;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final List<WizardStep> fields;

    protected AbstractWizardCommand(ProfileService profileService,
                                    KeyboardFactory keyboard,
                                    AbstractSectionCommand<?> sectionCommand,
                                    ProfileSelectionScreen profileSelectionScreen) {
        this.profileService = profileService;
        this.keyboard = keyboard;
        this.sectionCommand = sectionCommand;
        this.profileSelectionScreen = profileSelectionScreen;
        this.fields = WizardReflection.extractSteps(formClass());
    }

    /**
     * Класс свойств анкеты: из него рефлексией вычисляются шаги.
     */
    protected abstract Class<P> formClass();

    /**
     * Callback-действие кнопки, запускающей анкету (SYMPTOM_LOG / MEDICATION_COURSE / ...).
     */
    protected abstract CallbackAction startCallbackAction();

    /**
     * Сохранение заполненных свойств, возвращает результат сохраняющему сервису.
     */
    protected abstract Object save(P properties, Integer profileId, Long createdBy);

    /**
     * Текст уведомления об успешном сохранении.
     */
    protected abstract String savedMessage();

    /**
     * Текст уведомления об отмене.
     */
    protected abstract String cancelledMessage();

    @Override
    public final Set<CallbackAction> callbackActions() {
        return Set.of(startCallbackAction());
    }

    @Override
    public final Object handleCallback(Update update, User user, ReplySender reply, Object marker) {
        CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        WizardMarker wizard = marker instanceof WizardMarker w ? w : null;

        if (action == startCallbackAction()) {
            return start(user, reply);
        }

        if (wizard == null) {
            return null;
        }

        return switch (action) {
            case WIZARD_CONFIRM -> confirm(user, wizard, reply);
            case WIZARD_RETRY -> retry(user, wizard, reply);
            case WIZARD_CANCEL -> cancel(user, wizard, reply);
            case WIZARD_SKIP -> advance(user, wizard.withAnswer(currentField(wizard).fieldName(), null), reply);
            case WIZARD_BOOL_YES -> advance(user, wizard.withAnswer(currentField(wizard).fieldName(), Boolean.TRUE), reply);
            case WIZARD_BOOL_NO -> advance(user, wizard.withAnswer(currentField(wizard).fieldName(), Boolean.FALSE), reply);
            default -> null;
        };
    }

    @Override
    public final Object handlePendingText(Update update, User user, ReplySender reply, Object marker) {
        if (!(marker instanceof WizardMarker wizard)) {
            return null;
        }

        String raw = update.getMessage().getText().trim();
        WizardStep step = currentField(wizard);
        Class<?> type = step.type();

        Object value = WizardValidator.convertIfValid(raw, type);
        if (value == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.WIZARD_INVALID_INPUT)
                    .build());
            prompt(user, wizard, reply);
            return wizard;
        }

        return advance(user, wizard.withAnswer(step.fieldName(), value), reply);
    }

    // ===== Шаги =====

    private Object start(User user, ReplySender reply) {
        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            return null;
        }
        WizardMarker wizard = new WizardMarker(formClass(), 0, new HashMap<>(), profile.getId());
        prompt(user, wizard, reply);
        return wizard;
    }

    private Object advance(User user, WizardMarker wizard, ReplySender reply) {
        if (wizard.currentStep() + 1 < fields.size()) {
            WizardMarker next = wizard.withCurrentStep(wizard.currentStep() + 1);
            prompt(user, next, reply);
            return next;
        }
        showSummary(user, wizard, reply);
        return wizard;
    }

    private Object confirm(User user, WizardMarker wizard, ReplySender reply) {
        try {
            save(toProperties(wizard), wizard.profileId(), user.getId());
        } catch (RecordOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(e.getMessage())
                    .build());
            profileSelectionScreen.render(user, reply);
            return null;
        }
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(savedMessage())
                .build());
        sectionCommand.showSection(user, reply);
        return null;
    }

    private Object retry(User user, WizardMarker wizard, ReplySender reply) {
        WizardMarker reset = new WizardMarker(formClass(), 0, new HashMap<>(), wizard.profileId());
        prompt(user, reset, reply);
        return reset;
    }

    private Object cancel(User user, WizardMarker wizard, ReplySender reply) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(cancelledMessage())
                .build());
        sectionCommand.showSection(user, reply);
        return null;
    }

    // ===== Отображение =====

    private void prompt(User user, WizardMarker wizard, ReplySender reply) {
        WizardStep step = currentField(wizard);
        String text = BotTexts.WIZARD_STEP_TEMPLATE.formatted(
                wizard.currentStep() + 1, fields.size(),
                HtmlUtils.bold(step.label()), WizardValidator.formatHint(step.type()));

        InlineKeyboardMarkup km = keyboard.formStepKeyboard(step.optional(), isBoolean(step.type()));
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(km)
                .build());
    }

    private void showSummary(User user, WizardMarker wizard, ReplySender reply) {
        StringBuilder body = new StringBuilder();
        for (WizardStep step : fields) {
            Object answer = wizard.answers().get(step.fieldName());
            String value = WizardValidator.format(answer);
            body.append("• ").append(step.label()).append(": ")
                    .append(HtmlUtils.escape(value))
                    .append('\n');
        }
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.WIZARD_SUMMARY.formatted(body.toString().strip()))
                .parseMode("HTML")
                .replyMarkup(keyboard.formConfirmKeyboard())
                .build());
    }

    // ===== Помощники =====

    private WizardStep currentField(WizardMarker wizard) {
        return fields.get(wizard.currentStep());
    }

    private boolean isBoolean(Class<?> type) {
        return type == Boolean.class || type == boolean.class;
    }

    @SuppressWarnings("unchecked")
    private P toProperties(WizardMarker wizard) {
        Map<String, Field> byName = new HashMap<>();
        for (Field field : formClass().getDeclaredFields()) {
            byName.put(field.getName(), field);
        }

        P properties;
        try {
            properties = formClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Нет доступного конструктора без аргументов у " + formClass().getSimpleName(), e);
        }

        for (WizardStep step : fields) {
            Object value = wizard.answers().get(step.fieldName());
            if (value == null) {
                continue;
            }
            try {
                Field f = byName.get(step.fieldName());
                f.setAccessible(true);
                f.set(properties, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Не удалось заполнить поле " + step.fieldName(), e);
            }
        }
        return properties;
    }
}
