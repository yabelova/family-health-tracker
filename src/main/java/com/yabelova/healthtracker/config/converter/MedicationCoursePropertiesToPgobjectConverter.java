package com.yabelova.healthtracker.config.converter;

import com.yabelova.healthtracker.domain.MedicationCourseProperties;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;

@WritingConverter
@SuppressWarnings("NullableProblems")
public class MedicationCoursePropertiesToPgobjectConverter implements Converter<MedicationCourseProperties, PGobject> {

    private final ObjectMapper objectMapper;

    public MedicationCoursePropertiesToPgobjectConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PGobject convert(MedicationCourseProperties source) {
        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(objectMapper.writeValueAsString(source));
            return pgo;
        } catch (JacksonException | SQLException e) {
            throw new IllegalStateException("Не удалось сериализовать MedicationCourseProperties в jsonb", e);
        }
    }
}
