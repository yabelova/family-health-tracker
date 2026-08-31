package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.repository.ProfileRepository.ProfileParticipant;
import com.yabelova.healthtracker.service.DataPrivacyService;
import com.yabelova.healthtracker.service.DataPrivacyService.Decision;
import com.yabelova.healthtracker.service.DataPrivacyService.PrivacyDecision;
import com.yabelova.healthtracker.service.DataPrivacyService.ProfileGroups;
import com.yabelova.healthtracker.service.DataPrivacyService.OwnerShared;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.Commands;
import com.yabelova.healthtracker.telegram.support.ConfirmationWords;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ParticipantName;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataPrivacyCommand implements BotCommand {

    private final DataPrivacyService dataPrivacyService;
    private final KeyboardFactory keyboard;

    /**
     * Состояние флоу удаления: снимок групп профилей + накапливаемые решения.
     */
    private record DeleteFlow(ProfileGroups profileGroups, List<PrivacyDecision> decisions) {
    }

    @Override
    public Set<String> textKeys() {
        return Set.of(Commands.DELETE_ALL_DATA.token());
    }

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(CallbackAction.PRIVACY_TRANSFER, CallbackAction.PRIVACY_REVOKE);
    }

    @Override
    public Object handleText(Update update, User user, ReplySender reply) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.DELETE_ALL_WARNING)
                .build());

        ProfileGroups profileGroups = dataPrivacyService.getAndGroupProfiles(user);
        return continueFlow(user, new DeleteFlow(profileGroups, List.of()), reply);
    }

    @Override
    public Object handlePendingText(Update update, User user, ReplySender reply, Object marker) {
        DeleteFlow flow = marker instanceof DeleteFlow existing ? existing : null;
        String raw = update.getMessage().getText().trim();

        if (!raw.equalsIgnoreCase(ConfirmationWords.DELETE)) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.DELETE_ALL_CANCELLED)
                    .build());
            return null;
        }

        if (flow == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(errorText(null))
                    .build());
            return null;
        }

        try {
            dataPrivacyService.applyDeletionPlan(user, flow.decisions());
        } catch (ProfileOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(errorText(e.getMessage()))
                    .build());
            return null;
        }

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.DELETE_ALL_SUCCESS.formatted(Commands.START.token()))
                .build());
        return null;
    }

    @Override
    public Object handleCallback(Update update, User user, ReplySender reply, Object marker) {
        String data = update.getCallbackQuery().getData();
        CallbackAction action = CallbackAction.fromData(data);
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .text(BotTexts.DELETE_ALL_ANSWER)
                .showAlert(false)
                .build());

        if (!(marker instanceof DeleteFlow flow)) {
            return null;
        }

        String[] payload = data.split(":", 3);
        if (payload.length < 2) {
            return flow;
        }
        Integer profileId = parseId(payload[1]);
        if (profileId == null) {
            return flow;
        }

        List<PrivacyDecision> decisions = new ArrayList<>(flow.decisions());
        if (action == CallbackAction.PRIVACY_TRANSFER && payload.length >= 3) {
            Long targetUserId = parseLongId(payload[2]);
            if (targetUserId != null) {
                decisions.add(new PrivacyDecision(profileId, Decision.TRANSFER, targetUserId));
            }
        } else if (action == CallbackAction.PRIVACY_REVOKE) {
            decisions.add(new PrivacyDecision(profileId, Decision.REVOKE, null));
        }

        return continueFlow(user, new DeleteFlow(flow.profileGroups(), decisions), reply);
    }

    /**
     * Показываем следующий нерешённый общий профиль, либо финальную сводку со словом подтверждения.
     */
    private Object continueFlow(User user, DeleteFlow flow, ReplySender reply) {
        Optional<OwnerShared> next = nextUndecided(flow);

        if (next.isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.DELETE_ALL_SUMMARY.formatted(profileLines(flow), ConfirmationWords.DELETE))
                    .parseMode("HTML")
                    .build());
            return flow;
        }

        OwnerShared ownerShared = next.get();
        List<ProfileParticipant> participants = ownerShared.participants();

        StringBuilder body = new StringBuilder();
        for (ProfileParticipant participant : participants) {
            body.append("• ").append(ParticipantName.of(participant)).append("\n");
        }
        body.append(BotTexts.DELETE_ALL_PROFILE_OPTIONS);

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.DELETE_ALL_PROFILE_PROMPT.formatted(
                        HtmlUtils.bold(ownerShared.profile().getName()), body))
                .parseMode("HTML")
                .replyMarkup(keyboard.transferChoices(participants,
                        CallbackAction.PRIVACY_TRANSFER, ownerShared.profile().getId(), true))
                .build());
        return flow;
    }

    private Optional<OwnerShared> nextUndecided(DeleteFlow flow) {
        return flow.profileGroups().ownerShared().stream()
                .filter(os -> flow.decisions().stream()
                        .noneMatch(d -> d.profileId().equals(os.profile().getId())))
                .findFirst();
    }

    private String profileLines(DeleteFlow flow) {
        List<String> lines = new ArrayList<>();

        for (Profile profile : flow.profileGroups().personal()) {
            lines.add(BotTexts.DELETION_LINE_PERSONAL.formatted(HtmlUtils.bold(profile.getName())));
        }
        for (Profile profile : flow.profileGroups().member()) {
            lines.add(BotTexts.DELETION_LINE_MEMBER.formatted(HtmlUtils.bold(profile.getName())));
        }
        for (OwnerShared ownerShared : flow.profileGroups().ownerShared()) {
            Integer profileId = ownerShared.profile().getId();
            PrivacyDecision decision = flow.decisions().stream()
                    .filter(d -> d.profileId().equals(profileId))
                    .findFirst()
                    .orElse(null);
            if (decision == null) {
                lines.add(BotTexts.DELETION_LINE_OWNER_PENDING.formatted(HtmlUtils.bold(ownerShared.profile().getName())));
            } else if (decision.decision() == Decision.TRANSFER) {
                String targetName = ownerShared.participants().stream()
                        .filter(p -> p.userId().equals(decision.targetUserId()))
                        .findFirst()
                        .map(ParticipantName::of)
                        .orElse(BotTexts.PRIVACY_TRANSFER_TARGET_UNKNOWN);
                lines.add(BotTexts.DELETION_LINE_OWNER_TRANSFERRED.formatted(
                        HtmlUtils.bold(ownerShared.profile().getName()), HtmlUtils.bold(targetName)));
            } else {
                lines.add(BotTexts.DELETION_LINE_OWNER_REVOKED.formatted(HtmlUtils.bold(ownerShared.profile().getName())));
            }
        }

        return String.join("\n", lines);
    }

    private String errorText(String errorMessage) {
        StringBuilder sb = new StringBuilder(BotTexts.DELETE_ALL_ERROR_HEAD);
        if (errorMessage != null && !errorMessage.isBlank()) {
            sb.append('\n').append(errorMessage);
        }
        sb.append("\n\n")
                .append(BotTexts.DELETE_ALL_ERROR_RETRY.formatted(Commands.DELETE_ALL_DATA.token()))
                .append("\n\n")
                .append(BotTexts.DELETE_ALL_ERROR_MENU);
        return sb.toString();
    }

    private Integer parseId(String raw) {
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongId(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
