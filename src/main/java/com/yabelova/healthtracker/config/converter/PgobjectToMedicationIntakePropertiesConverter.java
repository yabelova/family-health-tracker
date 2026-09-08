package com.yabelova.healthtracker.config.converter;

import com.yabelova.healthtracker.domain.MedicationIntakeProperties;
import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ReadingConverter
@SuppressWarnings("NullableProblems")
public class PgobjectToMedicationIntakePropertiesConverter implements Converter<PGobject, MedicationIntakeProperties> {

    private final ObjectMapper objectMapper;

    public PgobjectToMedicationIntakePropertiesConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public MedicationIntakeProperties convert(PGobject source) {
        try {
            return objectMapper.readValue(source.getValue(), MedicationIntakeProperties.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Не удалось распарсить properties в MedicationIntakeProperties", e);
        }
    }
}
