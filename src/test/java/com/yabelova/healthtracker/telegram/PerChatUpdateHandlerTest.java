package com.yabelova.healthtracker.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тесты обработки обновлений по чатам: последовательность в одном чате, параллельность между чатами.
 */
class PerChatUpdateHandlerTest {

    private BotDispatcher dispatcher;
    private ExecutorService testExecutor;
    private PerChatUpdateHandler handler;

    @BeforeEach
    void setUp() {
        dispatcher = mock(BotDispatcher.class);
        testExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("test-", 0).factory());
        handler = new PerChatUpdateHandler(dispatcher, testExecutor);
    }

    @AfterEach
    void tearDown() {
        testExecutor.close();
    }

    @Test
    void multipleUpdatesSameChatProcessedInOrder() {
        List<String> processed = Collections.synchronizedList(new ArrayList<>());
        Mockito.doAnswer(inv -> processed.add(((Update) inv.getArgument(0)).getMessage().getText()))
                .when(dispatcher).dispatch(any());

        for (int i = 0; i < 5; i++) {
            handler.dispatch(createMessageUpdate(42L, "msg" + i));
        }

        verify(dispatcher, timeout(5000).times(5)).dispatch(any());
        assertThat(processed).containsExactly("msg0", "msg1", "msg2", "msg3", "msg4");
    }

    @Test
    void differentChatsProcessedInParallel() {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxConcurrent = new AtomicInteger();
        Mockito.doAnswer(inv -> {
            int current = active.incrementAndGet();
            maxConcurrent.updateAndGet(prev -> Math.max(prev, current));
            Thread.sleep(20);
            active.decrementAndGet();
            return null;
        }).when(dispatcher).dispatch(any());

        for (int i = 0; i < 10; i++) {
            handler.dispatch(createMessageUpdate((long) i, "msg"));
        }

        verify(dispatcher, timeout(5000).times(10)).dispatch(any());
        assertThat(maxConcurrent.get()).isGreaterThan(1);
    }

    @Test
    void rejectedExecutionOnShutdownLoggedAndIgnored() {
        ExecutorService shutdownExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("shutdown-", 0).factory());
        shutdownExecutor.close();
        PerChatUpdateHandler shutdownHandler = new PerChatUpdateHandler(dispatcher, shutdownExecutor);

        Update update = createMessageUpdate(200L, "shutdown-test");
        shutdownHandler.dispatch(update);

        verify(dispatcher, times(0)).dispatch(any());
    }

    @Test
    void updatesWithNullChatIdIgnored() {
        Update update = new Update();
        handler.dispatch(update);
        verify(dispatcher, times(0)).dispatch(any());
    }

    @Test
    void callbackQueryExtractsChatIdFromMessage() {
        Update update = createCallbackUpdate(300L, "callback_data");
        handler.dispatch(update);
        verify(dispatcher, timeout(5000).times(1)).dispatch(update);
    }

    private Update createMessageUpdate(Long chatId, String text) {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat(chatId, "private");
        message.setChat(chat);
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    private Update createCallbackUpdate(Long chatId, String data) {
        Update update = new Update();
        CallbackQuery callback = new CallbackQuery();
        callback.setData(data);
        Message message = new Message();
        Chat chat = new Chat(chatId, "private");
        message.setChat(chat);
        callback.setMessage(message);
        update.setCallbackQuery(callback);
        return update;
    }
}
