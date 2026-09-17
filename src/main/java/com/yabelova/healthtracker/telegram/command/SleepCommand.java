package com.yabelova.healthtracker.telegram.command;

import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.telegram.support.Commands;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;

/**
 * Временная демо-команда для проверки параллелизма обработки апдейтов: засыпает в потоке своего
 * чата на {@code app.demo.sleep.seconds} секунд.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.sleep.enabled", havingValue = "true", matchIfMissing = false)
public class SleepCommand implements BotCommand {

    private final ReplySender reply;

    @Value("${app.demo.sleep.seconds:60}")
    private int sleepSeconds;

    private static final String SLEEP_START = "😴 Засыпаю на %d сек. Этот чат заблокирован, другие чаты работают.";
    private static final String SLEEP_END = "😴 Проспал %d сек. Чат разблокирован.";

    @Override
    public Set<String> textKeys() {
        return Set.of(Commands.SLEEP.token());
    }

    @Override
    public Object handleText(Update update, User user) {
        reply.send(user, SLEEP_START.formatted(sleepSeconds));
        try {
            Thread.sleep(sleepSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        reply.send(user, SLEEP_END.formatted(sleepSeconds));
        return null;
    }
}
