package com.yabelova.healthtracker.config;

import com.yabelova.healthtracker.config.converter.MedicationCoursePropertiesToPgobjectConverter;
import com.yabelova.healthtracker.config.converter.MedicationIntakePropertiesToPgobjectConverter;
import com.yabelova.healthtracker.config.converter.PgobjectToMedicationCoursePropertiesConverter;
import com.yabelova.healthtracker.config.converter.PgobjectToMedicationIntakePropertiesConverter;
import com.yabelova.healthtracker.config.converter.PgobjectToSymptomPropertiesConverter;
import com.yabelova.healthtracker.config.converter.SymptomPropertiesToPgobjectConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Централизованно регистрирует конвертеры Spring Data JDBC для jsonb-колонки {@code properties},
 * которые в доменной модели представлены типизированными дата-классами:
 * {@code SymptomProperties}, {@code MedicationCourseProperties}, {@code MedicationIntakeProperties}.
 * Spring Data JDBC маппит sql-{@code jsonb} только на {@code PGobject}, для получения дата-классов
 * нужны кастомные конвертеры.
 */
@Configuration
public class JdbcConvertersConfig extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    public JdbcConvertersConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        return List.of(
                new PgobjectToSymptomPropertiesConverter(objectMapper),
                new SymptomPropertiesToPgobjectConverter(objectMapper),

                new PgobjectToMedicationCoursePropertiesConverter(objectMapper),
                new MedicationCoursePropertiesToPgobjectConverter(objectMapper),

                new PgobjectToMedicationIntakePropertiesConverter(objectMapper),
                new MedicationIntakePropertiesToPgobjectConverter(objectMapper)
        );
    }
}
