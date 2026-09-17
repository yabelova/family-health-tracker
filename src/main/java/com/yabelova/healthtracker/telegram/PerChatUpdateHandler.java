package com.yabelova.healthtracker.telegram;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Декоратор над {@link BotDispatcher}: упорядочивает апдейты одного чата и исполняет их в виртуальных потоках.
 * <p>
 * Между чатами параллельно, внутри {@code chatId} последовательно.
 */
@Slf4j
public class PerChatUpdateHandler implements UpdateHandler {

    private static final class ChatState {
        final Queue<Update> queue = new ConcurrentLinkedQueue<>();
        final AtomicBoolean processing = new AtomicBoolean();
    }

    private final BotDispatcher delegate;
    private final ExecutorService chatTaskExecutor;
    private final ConcurrentHashMap<Long, ChatState> chatStates = new ConcurrentHashMap<>();

    public PerChatUpdateHandler(BotDispatcher delegate, ExecutorService chatTaskExecutor) {
        this.delegate = delegate;
        this.chatTaskExecutor = chatTaskExecutor;
    }

    @Override
    public void dispatch(Update update) {
        Long chatId = chatIdOf(update);
        if (chatId == null) {
            return;
        }
        ChatState state = chatStates.computeIfAbsent(chatId, k -> new ChatState());
        state.queue.offer(update);
        if (state.processing.compareAndSet(false, true)) {
            log.debug("chat={} стартует обработка очереди", chatId);
            submitProcessQueue(chatId, state);
        }
    }

    private void processQueue(Long chatId, ChatState state) {
        Thread.currentThread().setName("task-chat-" + chatId);
        try {
            Queue<Update> queue = state.queue;
            Update update;
            while ((update = queue.poll()) != null) {
                try {
                    delegate.dispatch(update);
                } catch (Exception e) {
                    log.error("Ошибка обработки Update от chat={}", chatId, e);
                }
            }
        } finally {
            state.processing.set(false);
            // гонка poll→null / новый offer: апдейт мог прийти, пока снимали флаг
            if (!state.queue.isEmpty() && state.processing.compareAndSet(false, true)) {
                log.debug("chat={} гонка poll null / новый offer, повторная обработка очереди", chatId);
                submitProcessQueue(chatId, state);
            }
        }
    }

    private Long chatIdOf(Update update) {
        if (update.hasCallbackQuery()) {
            var message = update.getCallbackQuery().getMessage();
            return message != null ? message.getChatId() : null;
        }
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        return null;
    }

    private void submitProcessQueue(Long chatId, ChatState state) {
        try {
            chatTaskExecutor.submit(() -> processQueue(chatId, state));
        } catch (RejectedExecutionException e) {
            // executor закрыт (shutdown): апдейт не обработается, допустимо
            log.warn("chat={} не обработан: executor уже закрыт", chatId);
        }
    }
}
