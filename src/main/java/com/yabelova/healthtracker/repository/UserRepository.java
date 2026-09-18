package com.yabelova.healthtracker.repository;

import com.yabelova.healthtracker.domain.User;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends ListCrudRepository<User, Integer> {

    Optional<User> findByTelegramId(Long telegramId);

    /**
     * Пользователи, которым положена рассылка плана в момент {@code now}: время наступило (не позже {@code now}),
     * и на день {@code now} план еще не отправлен.
     */
    @Query("""
                SELECT * FROM t_users
                WHERE notification_time IS NOT NULL
                  AND notification_time <= CAST(:now AS time)
                  AND (last_notified_date IS NULL OR last_notified_date < CAST(:now AS date))
            """)
    List<User> findPlanRecipients(@Param("now") LocalDateTime now);

    /**
     * Точечная пометка - напоминание отправлено сегодня.
     */
    @Modifying
    @Query("""
                UPDATE t_users
                SET last_notified_date = CAST(:day AS date)
                WHERE id = :id
            """)
    void markNotified(@Param("id") Integer id, @Param("day") LocalDate day);

    /**
     * Точечное обновление времени напоминания и пометки о последней отправке.
     */
    @Modifying
    @Query("""
                UPDATE t_users
                SET notification_time = CAST(:time AS time),
                    last_notified_date = CAST(:lastNotified AS date)
                WHERE id = :id
            """)
    void updateNotificationTime(@Param("id") Integer id,
                                @Param("time") LocalTime time,
                                @Param("lastNotified") LocalDate lastNotified);

    /**
     * Сброс времени напоминания при отключении ежедневной рассылки.
     */
    @Modifying
    @Query("""
                UPDATE t_users
                SET notification_time = NULL
                WHERE id = :id
            """)
    void clearNotificationTime(@Param("id") Integer id);
}
