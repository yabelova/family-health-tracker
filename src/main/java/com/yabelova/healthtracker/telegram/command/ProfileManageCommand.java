package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.exception.ProfileOperationException;
import com.yabelova.healthtracker.service.ProfileService;
import com.yabelova.healthtracker.telegram.BotCommand;
import com.yabelova.healthtracker.telegram.CallbackAction;
import com.yabelova.healthtracker.telegram.screen.ProfileSelectionScreen;
import com.yabelova.healthtracker.telegram.support.HtmlUtils;
import com.yabelova.healthtracker.telegram.support.KeyboardFactory;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProfileManageCommand implements BotCommand {

    private final ProfileService profileService;
    private final ProfileSelectionScreen profileSelectionScreen;
    private final KeyboardFactory keyboard;

    @Override
    public Set<CallbackAction> callbackActions() {
        return Set.of(
                CallbackAction.PROFILE_MANAGE,
                CallbackAction.PROFILE_SHARE,
                CallbackAction.PROFILE_REVOKE,
                CallbackAction.PROFILE_DELETE,
                CallbackAction.PROFILE_RENAME
        );
    }

    @Override
    public Object handlePendingText(Update update, User user, ReplySender reply, Object marker) {
        CallbackAction flow = marker instanceof String data ? CallbackAction.fromData(data) : null;
        if (flow == CallbackAction.PROFILE_DELETE) {
            return handleDeleteConfirmation(update, user, reply, marker);
        }
        return handleRename(update, user, reply, marker);
    }

    @Override
    public Object handleCallback(Update update, User user, ReplySender reply) {
        String data = update.getCallbackQuery().getData();
        CallbackAction action = CallbackAction.fromData(data);
        reply.send(AnswerCallbackQuery.builder()
                .callbackQueryId(update.getCallbackQuery().getId())
                .build());

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("⚠️ Сначала выберите профиль")
                    .build());
            return null;
        }

        try {
            switch (action) {
                case PROFILE_MANAGE -> {
                    renderManage(user, profile, reply);
                    return null;
                }

                case PROFILE_SHARE -> {
                    String code = profileService.createInvite(user, profile.getId());
                    reply.send(SendMessage.builder()
                            .chatId(user.getId().toString())
                            .text("🔗 Код приглашения для профиля " + HtmlUtils.bold(profile.getName()) + ":\n\n"
                                    + HtmlUtils.bold(code) + "\n\n"
                                    + "Действует 72 часа и может быть использован один раз. Второй человек вводит его в "
                                    + "«👤 Выбор профиля» → «➕ Добавить по коду»")
                            .parseMode("HTML")
                            .build());
                    renderManage(user, profile, reply);
                    return null;
                }

                case PROFILE_RENAME -> {
                    reply.send(SendMessage.builder()
                            .chatId(user.getId().toString())
                            .text("Введите новое имя профиля:")
                            .build());
                    return CallbackAction.PROFILE_RENAME.prefix(); // ожидаем следующий текст (имя)
                }

                case PROFILE_REVOKE -> {
                    profileService.revokeAccess(user, profile.getId());
                    reply.send(SendMessage.builder()
                            .chatId(user.getId().toString())
                            .text("🚫 Доступ отозван у всех участников, неиспользованные коды удалены")
                            .build());
                    renderManage(user, profile, reply);
                    return null;
                }

                case PROFILE_DELETE -> {
                    reply.send(SendMessage.builder()
                            .chatId(user.getId().toString())
                            .text("⚠️ Удалить профиль " + HtmlUtils.bold(profile.getName()) + "? Это действие необратимо.\n"
                                    + "Чтобы подтвердить, введите слово " + HtmlUtils.bold("удалить")
                                    + " (любой другой текст отменит действие)")
                            .parseMode("HTML")
                            .build());
                    return CallbackAction.PROFILE_DELETE.prefix(); // ожидаем слово подтверждения или отмены
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

    private Object handleRename(Update update, User user, ReplySender reply, Object marker) {
        String name = update.getMessage().getText().trim();

        if (name.isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("Имя профиля не может быть пустым. Введите название:")
                    .build());
            return marker;
        }

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("⚠️ Сначала выберите профиль")
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
                .text("Профиль переименован в " + HtmlUtils.bold(name) + " ✅")
                .parseMode("HTML")
                .build());

        renderManage(user, profile, reply);
        return null;
    }

    private Object handleDeleteConfirmation(Update update, User user, ReplySender reply, Object marker) {
        String raw = update.getMessage().getText().trim();

        if (raw.isEmpty()) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("Чтобы подтвердить удаление, введите слово «удалить». Любой другой текст отменит действие:")
                    .build());
            return marker;
        }

        Profile profile = profileService.getActiveProfile(user);
        if (profile == null) {
            reply.send(SendMessage.builder()
                    .chatId(user.getId().toString())
                    .text("⚠️ Сначала выберите профиль")
                    .build());
            return null;
        }

        if (raw.equalsIgnoreCase("удалить")) {
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
                    .text("🗑 Профиль " + HtmlUtils.bold(profile.getName()) + " удалён")
                    .parseMode("HTML")
                    .build());
            profileSelectionScreen.render(user, reply);
            return null;
        }

        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text("🗑 Удаление отменено")
                .build());
        renderManage(user, profile, reply);
        return null;
    }

    private void renderManage(User user, Profile profile, ReplySender reply) {
        reply.send(SendMessage.builder()
                .chatId(user.getId().toString())
                .text("⚙️ Управление профилем " + HtmlUtils.bold(profile.getName()) + "\n\n"
                        + "Здесь вы можете:\n"
                        + "• 🔗 Поделиться — выдать одноразовый код-приглашение (действует 72 часа)\n"
                        + "• ✏️ Переименовать профиль\n"
                        + "• 🚫 Отменить доступ — отозвать доступ у всех участников и удалить коды\n"
                        + "• 🗑 Удалить профиль — безвозвратно")
                .parseMode("HTML")
                .replyMarkup(keyboard.manageMenu())
                .build());
    }
}