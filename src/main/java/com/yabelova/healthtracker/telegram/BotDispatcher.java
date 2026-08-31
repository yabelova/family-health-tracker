package com.yabelova.healthtracker.telegram;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.service.UserService;
import com.yabelova.healthtracker.telegram.command.BotCommand;
import com.yabelova.healthtracker.telegram.support.BotTexts;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Маршрутизирует входящие Update к командам. Строит карты по явным ключам команд
 * (тексты/команды и callback-действия), что делает маршрутизацию детерминированной
 */
@Component
@Slf4j
public class BotDispatcher {

    private final UserService userService;
    private final ReplySender reply;

    private final Map<String, BotCommand> textCommands = new HashMap<>();
    private final Map<CallbackAction, BotCommand> callbackCommands = new HashMap<>();
    private final Map<Long, AwaitingRequest> awaitingInput = new HashMap<>();

    public BotDispatcher(UserService userService,
                         ReplySender reply,
                         List<BotCommand> commands) {
        this.userService = userService;
        this.reply = reply;

        commands.forEach(cmd -> {
            cmd.textKeys().forEach(key -> putText(key, cmd));
            cmd.callbackActions().forEach(action -> putCallback(action, cmd));
        });

        log.info("Зарегистрировано команд: {} (тексты), {} (callback)",
                textCommands.size(), callbackCommands.size());
    }

    public void dispatch(Update update) {
        TelegramInput input = parse(update);
        if (input == null) {
            return;
        }
        if (input.callback()) {
            log.info("Обработан callback: chat={} data={}", input.chatId(), input.text());
        } else {
            log.info("Обработан текст: chat={} len={} text={}", input.chatId(), input.text().length(), input.text());
        }
        User user = getOrCreateUser(input);

        Route route = route(update, user.getId());
        BotCommand active = route.command();

        if (route.kind() == RouteKind.CALLBACK) {
            // удаляем клавиатуру сообщения, с которого пришёл клик — кнопки одноразовые
            var message = update.getCallbackQuery().getMessage();
            if (message != null) {
                reply.removeKeyboard(input.chatId(), message.getMessageId());
            }
        }

        Object next = switch (route.kind()) {
            case CALLBACK -> active.handleCallback(update, user, reply, route.marker());
            case REPLY -> active.handleText(update, user, reply);
            case PENDING -> active.handlePendingText(update, user, reply, route.marker());
            case NONE -> null;
        };

        if (next != null) {
            awaitingInput.put(input.chatId(), new AwaitingRequest(active, next));
            log.info("Ожидание ввода: chat={} cmd={} marker={}", input.chatId(),
                    active.getClass().getSimpleName(), markerLabel(next));
        } else {
            awaitingInput.remove(input.chatId());
        }
    }

    private Route route(Update update, Long chatId) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            CallbackAction action = CallbackAction.fromData(data);
            BotCommand callbackCmd = action != null ? callbackCommands.get(action) : null;
            if (callbackCmd == null) {
                awaitingInput.remove(chatId);
                log.warn("Неизвестное callback-действие [{}] от пользователя [{}]", data, chatId);
                replyUnexpected(chatId);
                return Route.none();
            }
            AwaitingRequest pending = awaitingInput.get(chatId);
            return Route.callback(callbackCmd, pending != null ? pending.marker() : null);
        }

        String text = textOf(update);
        BotCommand textCmd = textCommands.get(text);
        if (textCmd != null) {
            awaitingInput.remove(chatId);
            return Route.reply(textCmd);
        }

        AwaitingRequest pending = awaitingInput.get(chatId);
        if (pending != null) {
            return Route.pending(pending.command(), pending.marker());
        }

        log.warn("Не обрабатываемый ввод: chat={} len={}", chatId, text.length());
        replyUnexpected(chatId);
        return Route.none();
    }

    private void putText(String key, BotCommand command) {
        if (textCommands.put(key, command) != null) {
            throw new IllegalStateException("Дублирующийся текстовый ключ: " + key);
        }
    }

    private void putCallback(CallbackAction action, BotCommand command) {
        if (callbackCommands.put(action, command) != null) {
            throw new IllegalStateException("Дублирующееся callback-действие: " + action);
        }
    }

    private void replyUnexpected(Long chatId) {
        reply.send(SendMessage.builder()
                .chatId(chatId.toString())
                .text(BotTexts.COMMON_UNKNOWN_COMMAND)
                .build());
    }

    private String textOf(Update update) {
        return update.getMessage().getText() != null ? update.getMessage().getText() : "";
    }

    private String markerLabel(Object marker) {
        if (marker == null) {
            return "null";
        }
        return marker.getClass().getSimpleName();
    }

    private TelegramInput parse(Update update) {
        if (update.hasCallbackQuery()) {
            var c = update.getCallbackQuery();
            return new TelegramInput(c.getMessage().getChatId(), c.getData(), true,
                    c.getFrom().getFirstName(), c.getFrom().getUserName());
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            var m = update.getMessage();
            return new TelegramInput(m.getChatId(), m.getText(), false,
                    m.getFrom().getFirstName(), m.getFrom().getUserName());
        }
        // Стикеры, фото, служебные апдейты (my_chat_member и т.д.) — молча игнорируем
        return null;
    }

    private User getOrCreateUser(TelegramInput input) {
        return userService.getOrCreate(input.chatId(), input.firstName(), input.userName());
    }

    private record TelegramInput(Long chatId, String text, boolean callback,
                                 String firstName, String userName) {
    }

    private enum RouteKind {
        CALLBACK, REPLY, PENDING, NONE
    }

    private record AwaitingRequest(BotCommand command, Object marker) {
    }

    private record Route(RouteKind kind, BotCommand command, Object marker) {

        static Route none() {
            return new Route(RouteKind.NONE, null, null);
        }

        static Route callback(BotCommand command, Object marker) {
            return new Route(RouteKind.CALLBACK, command, marker);
        }

        static Route reply(BotCommand command) {
            return new Route(RouteKind.REPLY, command, null);
        }

        static Route pending(BotCommand command, Object marker) {
            return new Route(RouteKind.PENDING, command, marker);
        }
    }
}
