package com.yabelova.healthtracker.config.converter;

import com.yabelova.healthtracker.domain.SymptomLogProperties;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;

@WritingConverter
@SuppressWarnings("NullableProblems")
public class SymptomPropertiesToPgobjectConverter implements Converter<SymptomLogProperties, PGobject> {

    private final ObjectMapper objectMapper;

    public SymptomPropertiesToPgobjectConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PGobject convert(SymptomLogProperties source) {
        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(objectMapper.writeValueAsString(source));
            return pgo;
        } catch (JacksonException | SQLException e) {
            throw new IllegalStateException("Не удалось сериализовать SymptomProperties в jsonb", e);
        }
    }
}
