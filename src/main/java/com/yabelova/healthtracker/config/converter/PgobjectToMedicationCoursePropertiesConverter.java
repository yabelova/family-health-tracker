package com.yabelova.healthtracker.config.converter;

import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ReadingConverter
@SuppressWarnings("NullableProblems")
public class PgobjectToMedicationCoursePropertiesConverter implements Converter<PGobject, MedicationCourseProperties> {

    private final ObjectMapper objectMapper;

    public PgobjectToMedicationCoursePropertiesConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public MedicationCourseProperties convert(PGobject source) {
        try {
            return objectMapper.readValue(source.getValue(), MedicationCourseProperties.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Не удалось распарсить properties в MedicationCourseProperties", e);
        }
    }
}
