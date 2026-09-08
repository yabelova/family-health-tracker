package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.repository.ProfileRepository.ProfileParticipant;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.ConfirmationWords;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ParticipantName;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Numbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileManageCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final KeyboardFactory keyboard;
    private final ReplySender reply;

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(
                CallbackAction.PROFILE_MANAGE,
                CallbackAction.PROFILE_SHARE,
                CallbackAction.PROFILE_REVOKE,
                CallbackAction.PROFILE_DELETE,
                CallbackAction.PROFILE_RENAME,
                CallbackAction.PROFILE_TRANSFER,
                CallbackAction.PROFILE_TRANSFER_OWNERSHIP
        );
    }

    @Override
    public Object handlePendingText(Update update, User user, Object marker) {
        if (marker instanceof TransferChoice choice) {
            return handleTransferConfirmation(update, user, choice);
        }
        if (!(marker instanceof CallbackAction action)) {
            return null;
        }
        return switch (action) {
            case PROFILE_DELETE -> handleDeleteConfirmation(update, user, marker);
            case PROFILE_RENAME -> handleRename(update, user, marker);
            default -> null;
        };
    }

    @Override
    public Object handleCallback(Update update, User user) {
        String data = update.getCallbackQuery().getData();
        CallbackAction action = CallbackAction.fromData(data);
        reply.answerCallbackQuery(update.getCallbackQuery().getId());

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            return null;
        }

        try {
            switch (action) {
                case PROFILE_MANAGE -> {
                    renderManage(user, profile);
                    return null;
                }

                case PROFILE_RENAME -> {
                    reply.send(SendMessage.builder()
                            .chatId(user.getId().toString())
                            .text(BotTexts.RENAME_PROMPT)
                            .build());
                    return CallbackAction.PROFILE_RENAME; // ожидаем следующий текст (имя)
                }

                case PROFILE_SHARE -> {
                    return handleShare(user, profile);
                }

                case PROFILE_REVOKE -> {
                    return handleRevoke(user, profile);
                }

                case PROFILE_TRANSFER -> {
                    renderTransferChoice(user, profile);
                    return null;
                }

                case PROFILE_TRANSFER_OWNERSHIP -> {
                    return handleTransferChoice(data, user, profile);
                }

                case PROFILE_DELETE -> {
                    reply.send(SendMessage.builder()
                            .chatId(user.getId().toString())
                            .text(BotTexts.DELETE_CONFIRM.formatted(
                                    HtmlUtils.bold(profile.getName()),
                                    HtmlUtils.bold(ConfirmationWords.DELETE)))
                            .parseMode("HTML")
                            .build());
                    return CallbackAction.PROFILE_DELETE; // ожидаем слово подтверждения или отмены
                }

                default -> {
                    return null;
                }
            }
        } catch (ProfileOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(e.getMessage())
                    .build());
            return null;
        }
    }

    private Object handleRename(Update update, User user, Object marker) {
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.RENAME_NAME_EMPTY)
                    .build());
            return marker;
        }

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            return null;
        }

        try {
            profileService.renameProfile(user, profile.getId(), name);
            profile.setName(name);
        } catch (ProfileOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(e.getMessage())
                    .build());
            return null;
        }

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.RENAME_SUCCESS.formatted(HtmlUtils.bold(name)))
                .parseMode("HTML")
                .build());

        renderManage(user, profile);
        return null;
    }

    private Object handleTransferConfirmation(Update update, User user, TransferChoice choice) {
        String raw = update.getMessage().getText().trim();

        if (!raw.equalsIgnoreCase(ConfirmationWords.TRANSFER)) {
            Profile profile = profileService.getActiveProfile(user);
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.TRANSFER_CANCELLED)
                    .build());
            if (profile != null) {
                renderManage(user, profile);
            }
            return null;
        }

        try {
            profileService.transferOwnership(user, choice.profileId(), choice.targetUserId());
        } catch (ProfileOperationException e) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(e.getMessage())
                    .build());
            return null;
        }

        profileService.getActiveProfile(user);
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.TRANSFER_SUCCESS)
                .build());
        profileSelectionScreen.render(user);
        return null;
    }

    private Object handleDeleteConfirmation(Update update, User user, Object marker) {
        String raw = update.getMessage().getText().trim();

        if (raw.isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.DELETE_PROMPT_PROGRESS.formatted(ConfirmationWords.DELETE))
                    .build());
            return marker;
        }

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.COMMON_FIRST_SELECT_PROFILE)
                    .build());
            return null;
        }

        if (raw.equalsIgnoreCase(ConfirmationWords.DELETE)) {
            try {
                profileService.deleteProfile(user, profile.getId());
            } catch (ProfileOperationException e) {
                reply.send(SendMessage.builder()
                        .chatId(user.getId().toString())
                        .text(e.getMessage())
                        .build());
                return null;
            }
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.DELETE_SUCCESS.formatted(HtmlUtils.bold(profile.getName())))
                    .parseMode("HTML")
                    .build());
            profileSelectionScreen.render(user);
            return null;
        }

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.DELETE_CANCELLED)
                .build());
        renderManage(user, profile);
        return null;
    }


    private void renderManage(User user, Profile profile) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.MANAGE_MENU_HEAD.formatted(HtmlUtils.bold(profile.getName())))
                .parseMode("HTML")
                .replyMarkup(keyboard.manageMenu())
                .build());
    }

    private Object handleShare(User user, Profile profile) {
        String code = profileService.createInvite(user, profile.getId());
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.SHARE_CODE_TEXT.formatted(
                        HtmlUtils.bold(profile.getName()), HtmlUtils.bold(code)))
                .parseMode("HTML")
                .build());
        renderManage(user, profile);
        return null;
    }

    private Object handleRevoke(User user, Profile profile) {
        profileService.revokeAccess(user, profile.getId());
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.REVOKE_SUCCESS)
                .build());
        renderManage(user, profile);
        return null;
    }

    private void renderTransferChoice(User user, Profile profile) {
        List<ProfileParticipant> participants = profileService.participants(profile.getId());
        if (participants.isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text(BotTexts.TRANSFER_NO_PARTICIPANTS.formatted(HtmlUtils.bold(profile.getName())))
                    .parseMode("HTML")
                    .build());
            renderManage(user, profile);
            return;
        }
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.TRANSFER_PROMPT.formatted(HtmlUtils.bold(profile.getName())))
                .parseMode("HTML")
                .replyMarkup(keyboard.transferChoices(participants,
                        CallbackAction.PROFILE_TRANSFER_OWNERSHIP, profile.getId(), false))
                .build());
    }

    private Object handleTransferChoice(String data, User user, Profile profile) {
        String[] payload = data.split(":", 3);
        if (payload.length < 3) {
            return null;
        }
        Long targetUserId = Numbers.parseLong(payload[2]);
        if (targetUserId == null) {
            return null;
        }
        String targetName = profileService.participants(profile.getId()).stream()
                .filter(p -> p.userId().equals(targetUserId))
                .findFirst()
                .map(ParticipantName::of)
                .orElse(BotTexts.TRANSFER_TARGET_DATIVE);
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text(BotTexts.TRANSFER_CONFIRM.formatted(
                        HtmlUtils.bold(profile.getName()),
                        HtmlUtils.bold(targetName),
                        HtmlUtils.bold(ConfirmationWords.TRANSFER)))
                .parseMode("HTML")
                .build());
        return new TransferChoice(profile.getId(), targetUserId);
    }

    /**
     * Маркер ожидания слова «передать» после выбора нового владельца.
     */
    private record TransferChoice(Integer profileId, Long targetUserId) {
    }
}
