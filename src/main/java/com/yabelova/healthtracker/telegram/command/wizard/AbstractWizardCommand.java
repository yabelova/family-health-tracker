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
import com.yabelova.healthtracker.util.TimeZones;
import com.yabelova.healthtracker.wizard.WizardReflection;
import com.yabelova.healthtracker.wizard.WizardStep;
import com.yabelova.healthtracker.wizard.WizardValidator;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Общий каркас пошаговой анкеты (визарда). Реализует весь флоу: список шагов
 * строится рефлексией из {@link #formClass()} один раз, обработка текста и
 * универсальных wizard-кнопок, подтверждение/отмена, экран итогов.
 * <p>
 * Конкретная анкета наследует этот класс и задает только: класс полей, callback
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
    private final ReplySender reply;
    private final List<WizardStep> fields;

    protected AbstractWizardCommand(ProfileService profileService,
                                    KeyboardFactory keyboard,
                                    AbstractSectionCommand<?> sectionCommand,
                                    ProfileSelectionScreen profileSelectionScreen,
                                    ReplySender reply) {
        this.profileService = profileService;
        this.keyboard = keyboard;
        this.sectionCommand = sectionCommand;
        this.profileSelectionScreen = profileSelectionScreen;
        this.reply = reply;
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
    public final Object handleCallback(Update update, User user, Object marker) {
        CallbackAction action = CallbackAction.fromData(update.getCallbackQuery().getData());
        reply.answerCallbackQuery(update.getCallbackQuery().getId());

        WizardMarker wizard = marker instanceof WizardMarker w ? w : null;

        if (action == startCallbackAction()) {
            return start(user);
        }

        if (wizard == null) {
            return null;
        }

        return switch (action) {
            case WIZARD_CONFIRM -> confirm(user, wizard);
            case WIZARD_RETRY -> retry(user, wizard);
            case WIZARD_CANCEL -> cancel(user, wizard);
            case WIZARD_SKIP -> advance(user, wizard.withAnswer(currentField(wizard).fieldName(), null));
            case WIZARD_QUICK_SET -> quickSet(update, user, wizard);
            case WIZARD_BOOL_YES -> advance(user, wizard.withAnswer(currentField(wizard).fieldName(), Boolean.TRUE));
            case WIZARD_BOOL_NO -> advance(user, wizard.withAnswer(currentField(wizard).fieldName(), Boolean.FALSE));
            default -> null;
        };
    }

    @Override
    public final Object handlePendingText(Update update, User user, Object marker) {
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
            prompt(user, wizard);
            return wizard;
        }

        return advance(user, wizard.withAnswer(step.fieldName(), value));
    }

    // ===== Шаги =====

    private Object start(User user) {
        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            return null;
        }
        WizardMarker wizard = new WizardMarker(formClass(), 0, new HashMap<>(), profile.getId());
        prompt(user, wizard);
        return wizard;
    }

    private Object advance(User user, WizardMarker wizard) {
        if (wizard.currentStep() + 1 < fields.size()) {
            WizardMarker next = wizard.withCurrentStep(wizard.currentStep() + 1);
            prompt(user, next);
            return next;
        }
        showSummary(user, wizard);
        return wizard;
    }

    private Object confirm(User user, WizardMarker wizard) {
        try {
            save(toProperties(wizard), wizard.profileId(), user.getId());
        } catch (RecordOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(e.getMessage())
                    .build());
            profileSelectionScreen.render(user);
            return null;
        }
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(savedMessage())
                .build());
        sectionCommand.showSection(user);
        return null;
    }

    private Object retry(User user, WizardMarker wizard) {
        WizardMarker reset = new WizardMarker(formClass(), 0, new HashMap<>(), wizard.profileId());
        prompt(user, reset);
        return reset;
    }

    private Object cancel(User user, WizardMarker wizard) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(cancelledMessage())
                .build());
        sectionCommand.showSection(user);
        return null;
    }

    /**
     * Быстрая кнопка шага анкеты (Сегодня/Вчера/Завтра/Сейчас): подставляет
     * текущую дату/время в зависимости от типа поля текущего шага.
     */
    private Object quickSet(Update update, User user, WizardMarker wizard) {
        WizardStep step = currentField(wizard);
        String token = CallbackAction.payloadOf(update.getCallbackQuery().getData());
        LocalDate today = LocalDate.now(TimeZones.DEFAULT);

        Object value = switch (step.type()) {
            case Class<?> c when c == LocalDate.class -> switch (token) {
                case "today" -> today;
                case "yesterday" -> today.minusDays(1);
                case "tomorrow" -> today.plusDays(1);
                default -> null;
            };
            case Class<?> c when c == LocalDateTime.class ->
                    "now".equals(token) ? LocalDateTime.now(TimeZones.DEFAULT) : null;
            case Class<?> c when c == LocalTime.class ->
                    "now".equals(token) ? LocalTime.now(TimeZones.DEFAULT) : null;
            default -> null;
        };

        if (value == null) {
            return null;
        }
        return advance(user, wizard.withAnswer(step.fieldName(), value));
    }

    // ===== Отображение =====

    private void prompt(User user, WizardMarker wizard) {
        WizardStep step = currentField(wizard);
        String text = BotTexts.WIZARD_STEP_TEMPLATE.formatted(
                wizard.currentStep() + 1, fields.size(),
                HtmlUtils.bold(step.label()), WizardValidator.formatHint(step.type()));

        InlineKeyboardMarkup km = keyboard.formStepKeyboard(step.type(), step.optional());
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(km)
                .build());
    }

    private void showSummary(User user, WizardMarker wizard) {
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
