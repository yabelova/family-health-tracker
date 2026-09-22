package com.yabelova.healthtracker.integration;

import com.yabelova.healthtracker.domain.MedicationCourse;
import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import com.yabelova.healthtracker.domain.Profile;
import com.yabelova.healthtracker.domain.User;
import com.yabelova.healthtracker.domain.UserRole;
import com.yabelova.healthtracker.repository.MedicationCourseRepository;
import com.yabelova.healthtracker.repository.ProfileRepository;
import com.yabelova.healthtracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Общая база интеграционных тестов: контейнер Postgres, чистка таблиц и стаб отправителя сообщений.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractContainerIntTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));
        POSTGRES.start();
    }

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserRepository users;

    @Autowired
    ProfileRepository profiles;

    @Autowired
    MedicationCourseRepository courses;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void cleanAppTables() {
        jdbcTemplate.execute("""
                TRUNCATE t_symptom_logs, t_medication_intakes, t_medication_courses,
                         t_profile_invites, tr_user_profile, t_users, t_profiles
                RESTART IDENTITY CASCADE
                """);
    }

    protected Integer createUser(String firstName) {
        int n = SEQ.incrementAndGet();
        return users.save(User.builder()
                .telegramId(1_000_000L + n)
                .telegramFirstName(firstName)
                .createdAt(Instant.now())
                .build()).getId();
    }

    protected Integer createProfileLinked(Integer userId, boolean active) {
        int n = SEQ.incrementAndGet();
        Profile profile = profiles.save(Profile.builder()
                .name("Профиль " + n)
                .createdAt(Instant.now())
                .build());
        profiles.linkUserToProfile(userId, profile.getId(), UserRole.OWNER, active);
        return profile.getId();
    }

    protected MedicationCourse createActiveCourse(Integer profileId, Integer createdBy) {
        MedicationCourseProperties properties = new MedicationCourseProperties();
        properties.setMedication("Амоксициллин");
        properties.setDosesPerDay(2);
        properties.setDosesPerPackage(20);
        return courses.save(MedicationCourse.builder()
                .profileId(profileId)
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .properties(properties)
                .dosesTaken(0)
                .build());
    }

    @TestConfiguration
    static class SenderTestConfiguration {

        @Bean
        AbsSender absSender() {
            return Mockito.mock(AbsSender.class);
        }
    }
}
