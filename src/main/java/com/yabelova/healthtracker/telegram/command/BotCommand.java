package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.CallbackAction;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

/**
 * Одна команда бота: объявляет, каким входам отвечает (текст/команды и inline-кнопки),
 * и обрабатывает их. Диспетчер маршрутизирует по явным ключам.
 * <p>
 * Все методы — no-op по умолчанию (пустой набор / null): команда реализует только то,
 * что использует. Необъявленные ключи не маршрутизируются, вернувший null обработчик
 * не ставит ожидание, поэтому «забытая» реализация не роняет систему.
 * <p>
 * handleText — обработка нажатия reply-кнопки или slash-команды
 * handleCallback — обработка нажатия inline-кнопки
 * handlePendingText — обработка свободного текстового ввода, когда команда
 * ранее сообщила, что ожидает следующий текст.
 * <p>
 * Все handle возвращают маркер ожидания: если команда теперь ждёт следующий
 * свободный текстовый ввод или клик внутри того же флоу, она возвращает маркер,
 * под которым этот ввод будет различаться; иначе — null. Маркер — это либо
 * CallbackAction (вид флоу: какая кнопка его запустила), либо record с состоянием
 * флоу, которое нужно переносить между шагами (например, TransferChoice, ProfileGroups).
 * Команда сама знает своё состояние и шаги анкеты, диспетчер лишь хранит
 * возвращённый маркер и передаёт его обратно в следующий
 * handlePendingText/handleCallback
 */
public interface BotCommand {

    /**
     * Текстовые ключи: /start и/или точный текст reply-кнопок.
     */
    default Set<String> textKeys() {
        return Set.of();
    }

    /**
     * Inline-действия (callback), которые обрабатывает команда.
     */
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
     * под которым ожидание продолжится (например, следующий шаг анкеты)
     */
    default Object handlePendingText(Update update, User user, ReplySender reply, Object marker) {
        return null;
    }

    /**
     * Обработка нажатия inline-кнопки. Переопределяется командами, которым
     * текущий шаг анкеты не важен (большинство команд).
     *
     * @return null — не ждём следующий текст; не-null — маркер, под которым ждём
     */
    default Object handleCallback(Update update, User user, ReplySender reply) {
        return null;
    }

    /**
     * Обработка нажатия inline-кнопки с маркером ожидания. Диспетчер всегда вызывает
     * эту версию (маркер равен null, если ожидания нет); переопределять её стоит
     * только если кнопка должна отличать «свежий» клик от клика внутри своего флоу.
     * По умолчанию игнорирует маркер и делегирует в
     * {@link #handleCallback(Update, User, ReplySender)}.
     *
     * @param marker маркер, под которым команда ранее заявила ожидание (может быть null)
     * @return null — не ждём следующий текст; не-null — маркер, под которым ждём
     */
    default Object handleCallback(Update update, User user, ReplySender reply, Object marker) {
        return handleCallback(update, user, reply);
    }
}