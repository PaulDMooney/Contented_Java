package com.contented.contented.contentlet.persistence;

import com.contented.contented.contentlet.SchemalessData;
import org.postgresql.util.PGobject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers the {@link SchemalessData} &lt;-&gt; {@code jsonb} converters with Spring Data JDBC.
 * Extending {@link AbstractJdbcConfiguration} makes Spring Boot back off its own and use this one.
 */
@Configuration
@ConditionalOnProperty(prefix = "contented.persistence.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcConfig extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    public JdbcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        return List.of(
            new SchemalessDataWritingConverter(objectMapper),
            new SchemalessDataReadingConverter(objectMapper)
        );
    }

    @WritingConverter
    static class SchemalessDataWritingConverter implements Converter<SchemalessData, PGobject> {

        private final ObjectMapper objectMapper;

        SchemalessDataWritingConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public PGobject convert(SchemalessData source) {
            try {
                var pgObject = new PGobject();
                pgObject.setType("jsonb");
                pgObject.setValue(objectMapper.writeValueAsString(source.values()));
                return pgObject;
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to write schemaless data as jsonb", e);
            }
        }
    }

    @ReadingConverter
    static class SchemalessDataReadingConverter implements Converter<PGobject, SchemalessData> {

        private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
        };

        private final ObjectMapper objectMapper;

        SchemalessDataReadingConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public SchemalessData convert(PGobject source) {
            String json = source.getValue();
            if (json == null || json.isBlank()) {
                return new SchemalessData();
            }
            Map<String, Object> values = objectMapper.readValue(json, MAP_TYPE);
            return new SchemalessData(values);
        }
    }
}
