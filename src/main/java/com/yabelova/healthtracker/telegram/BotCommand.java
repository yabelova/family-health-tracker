package com.yabelova.healthtracker.telegram;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

/**
 * Одна команда бота: объявляет, каким входам отвечает (текст/команды и inline-кнопки),
 * и обрабатывает их. Диспетчер маршрутизирует по явным ключам (без findFirst).
 *
 * handleText — обработка нажатия reply-кнопки или slash-команды.
 * handlePendingText — обработка свободного текстового ввода, когда команда
 *     ранее сообщила (вернула true), что ожидает следующий текст.
 * handleCallback — обработка нажатия inline-кнопки.
 *
 * handle* возвращают true, если команда теперь ожидает следующий свободный
 * текстовый ввод пользователя (например, время или имя).
 */
public interface BotCommand {

    /** Текстовые ключи: /start и/или точный текст reply-кнопок. */
    Set<String> textKeys();

    /** Inline-действия (callback), которые обрабатывает команда. */
    Set<CallbackAction> callbackActions();

    boolean handleText(Update update, User user, ReplySender reply);

    boolean handlePendingText(Update update, User user, ReplySender reply);

    boolean handleCallback(Update update, User user, ReplySender reply);
}
