package com.yabelova.healthtracker.telegram;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

/**
 * Одна команда бота: объявляет, каким входам отвечает (текст/команды и inline-кнопки),
 * и обрабатывает их. Диспетчер маршрутизирует по явным ключам (без findFirst)
 *
 * Все методы — no-op по умолчанию (пустой набор / null): команда реализует только то,
 * что использует. Необъявленные ключи не маршрутизируются, вернувший null обработчик
 * не ставит ожидание, поэтому «забытая» реализация не роняет систему.
 *
 * handleText — обработка нажатия reply-кнопки или slash-команды
 * handlePendingText — обработка свободного текстового ввода, когда команда
 *     ранее сообщила, что ожидает следующий текст
 * handleCallback — обработка нажатия inline-кнопки
 *
 * Все handle* возвращают маркер ожидания: если команда теперь ждёт следующий
 * свободный текстовый ввод, она возвращает ярлык, под которым этот ввод будет
 * различаться (например, префикс callback-действия); иначе — null. Команда сама
 * знает своё состояние и шаги анкеты, диспетчер лишь хранит возвращённый маркер
 * и передаёт его обратно в следующий handlePendingText
 */
public interface BotCommand {

    /** Текстовые ключи: /start и/или точный текст reply-кнопок. */
    default Set<String> textKeys() {
        return Set.of();
    }

    /** Inline-действия (callback), которые обрабатывает команда. */
    default Set<CallbackAction> callbackActions() {
        return Set.of();
    }

    /**
     * Обработка нажатия reply-кнопки или slash-команды
     *
     * @return null — не ждём следующий текст; не-null — маркер, под которым ждём
     */
    default Object handleText(Update update, User user, ReplySender reply) {
        return null;
    }

    /**
     * Обработка свободного текстового ввода под сохранённым маркером флоу.
     *
     * @param marker ярлык, под которым команда ранее заявила ожидание
     * @return null — анкета завершена, ожидание снимается; не-null — новый маркер,
     *         под которым ожидание продолжится (например, следующий шаг анкеты)
     */
    default Object handlePendingText(Update update, User user, ReplySender reply, Object marker) {
        return null;
    }

    /**
     * Обработка нажатия inline-кнопки.
     *
     * @return null — не ждём следующий текст; не-null — маркер, под которым ждём
     */
    default Object handleCallback(Update update, User user, ReplySender reply) {
        return null;
    }
}