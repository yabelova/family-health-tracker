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
import com.yabelova.healthtracker.telegram.support.ParseMode;
import com.yabelova.healthtracker.telegram.support.ParticipantName;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import com.yabelova.healthtracker.util.Numbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            return null;
        }

        try {
            switch (action) {
                case PROFILE_MANAGE -> {
                    renderManage(user, profile);
                    return null;
                }

                case PROFILE_RENAME -> {
                    reply.send(user, BotTexts.RENAME_PROMPT);
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
                    reply.send(user, BotTexts.DELETE_CONFIRM.formatted(
                                    HtmlUtils.bold(profile.getName()),
                                    HtmlUtils.bold(ConfirmationWords.DELETE)),
                            ParseMode.HTML);
                    return CallbackAction.PROFILE_DELETE; // ожидаем слово подтверждения или отмены
                }

                default -> {
                    return null;
                }
            }
        } catch (ProfileOperationException e) {
            reply.send(user, e.getMessage());
            return null;
        }
    }

    private Object handleRename(Update update, User user, Object marker) {
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            reply.send(user, BotTexts.RENAME_NAME_EMPTY);
            return marker;
        }

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            return null;
        }

        try {
            profileService.renameProfile(user, profile.getId(), name);
            profile.setName(name);
        } catch (ProfileOperationException e) {
            reply.send(user, e.getMessage());
            return null;
        }

        reply.send(user, BotTexts.RENAME_SUCCESS.formatted(HtmlUtils.bold(name)), ParseMode.HTML);

        renderManage(user, profile);
        return null;
    }

    private Object handleTransferConfirmation(Update update, User user, TransferChoice choice) {
        String raw = update.getMessage().getText().trim();

        if (!raw.equalsIgnoreCase(ConfirmationWords.TRANSFER)) {
            Profile profile = profileService.getActiveProfile(user);
            reply.send(user, BotTexts.TRANSFER_CANCELLED);
            if (profile != null) {
                renderManage(user, profile);
            }
            return null;
        }

        try {
            profileService.transferOwnership(user, choice.profileId(), choice.targetUserId());
        } catch (ProfileOperationException e) {
            reply.send(user, e.getMessage());
            return null;
        }

        profileService.getActiveProfile(user);
        reply.send(user, BotTexts.TRANSFER_SUCCESS);
        profileSelectionScreen.render(user);
        return null;
    }

    private Object handleDeleteConfirmation(Update update, User user, Object marker) {
        String raw = update.getMessage().getText().trim();

        if (raw.isEmpty()) {
            reply.send(user, BotTexts.DELETE_PROMPT_PROGRESS.formatted(ConfirmationWords.DELETE));
            return marker;
        }

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(user, BotTexts.COMMON_FIRST_SELECT_PROFILE);
            return null;
        }

        if (raw.equalsIgnoreCase(ConfirmationWords.DELETE)) {
            try {
                profileService.deleteProfile(user, profile.getId());
            } catch (ProfileOperationException e) {
                reply.send(user, e.getMessage());
                return null;
            }
            reply.send(user, BotTexts.DELETE_SUCCESS.formatted(HtmlUtils.bold(profile.getName())), ParseMode.HTML);
            profileSelectionScreen.render(user);
            return null;
        }

        reply.send(user, BotTexts.DELETE_CANCELLED);
        renderManage(user, profile);
        return null;
    }


    private void renderManage(User user, Profile profile) {
        reply.send(user, BotTexts.MANAGE_MENU_HEAD.formatted(HtmlUtils.bold(profile.getName())),
                ParseMode.HTML, keyboard.manageMenu());
    }

    private Object handleShare(User user, Profile profile) {
        String code = profileService.createInvite(user, profile.getId());
        reply.send(user, BotTexts.SHARE_CODE_TEXT.formatted(
                        HtmlUtils.bold(profile.getName()), HtmlUtils.bold(code)),
                ParseMode.HTML);
        renderManage(user, profile);
        return null;
    }

    private Object handleRevoke(User user, Profile profile) {
        profileService.revokeAccess(user, profile.getId());
        reply.send(user, BotTexts.REVOKE_SUCCESS);
        renderManage(user, profile);
        return null;
    }

    private void renderTransferChoice(User user, Profile profile) {
        List<ProfileParticipant> participants = profileService.participants(profile.getId());
        if (participants.isEmpty()) {
            reply.send(user, BotTexts.TRANSFER_NO_PARTICIPANTS.formatted(HtmlUtils.bold(profile.getName())),
                    ParseMode.HTML);
            renderManage(user, profile);
            return;
        }
        reply.send(user, BotTexts.TRANSFER_PROMPT.formatted(HtmlUtils.bold(profile.getName())),
                ParseMode.HTML,
                keyboard.transferChoices(participants,
                        CallbackAction.PROFILE_TRANSFER_OWNERSHIP, profile.getId(), false));
    }

    private Object handleTransferChoice(String data, User user, Profile profile) {
        String[] payload = data.split(":", 3);
        if (payload.length < 3) {
            return null;
        }
        Integer targetUserId = Numbers.parseInt(payload[2]);
        if (targetUserId == null) {
            return null;
        }
        String targetName = profileService.participants(profile.getId()).stream()
                .filter(p -> p.userId().equals(targetUserId))
                .findFirst()
                .map(ParticipantName::of)
                .orElse(BotTexts.TRANSFER_TARGET_DATIVE);
        reply.send(user, BotTexts.TRANSFER_CONFIRM.formatted(
                        HtmlUtils.bold(profile.getName()),
                        HtmlUtils.bold(targetName),
                        HtmlUtils.bold(ConfirmationWords.TRANSFER)),
                ParseMode.HTML);
        return new TransferChoice(profile.getId(), targetUserId);
    }

    /**
     * Маркер ожидания слова «передать» после выбора нового владельца.
     */
    private record TransferChoice(Integer profileId, Integer targetUserId) {
    }
}
