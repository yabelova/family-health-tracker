package com.yabelova.healthtracker.config;

import com.yabelova.healthtracker.service.UserService;
import com.yabelova.healthtracker.telegram.BotDispatcher;
import com.yabelova.healthtracker.telegram.PerChatUpdateHandler;
import com.yabelova.healthtracker.telegram.UpdateHandler;
import com.yabelova.healthtracker.telegram.command.BotCommand;
import com.yabelova.healthtracker.telegram.support.ReplySender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableScheduling
public class ThreadingConfig {

    /**
     * Бин диспетчера команд, нужен всегда.
     */
    @Bean
    public BotDispatcher botDispatcher(UserService userService,
                                       ReplySender reply,
                                       TelegramAccessConfig access,
                                       List<BotCommand> commands) {
        return new BotDispatcher(userService, reply, access, commands);
    }

    /**
     * Режим с параллелизмом: декоратор над {@link BotDispatcher} - per-chat очереди + виртуальные потоки.
     * Включен по умолчанию. При {@code app.parallel.enabled=false} заменяется на {@link BotDispatcher}.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.parallel.enabled", havingValue = "true", matchIfMissing = true)
    public UpdateHandler perChatHandler(BotDispatcher dispatcher, ExecutorService chatTaskExecutor) {
        return new PerChatUpdateHandler(dispatcher, chatTaskExecutor);
    }

    /**
     * Исполнитель: запускает задачи (апдейты пользователей) в отдельных виртуальных потоках.
     */
    @Bean
    public ExecutorService chatTaskExecutor() {
        ThreadFactory factory = Thread.ofVirtual().name("task-", 0).factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }
}
