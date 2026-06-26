package com.blog.cms.content;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter for List&lt;String&gt; stored as JSON in a single TEXT column.
 *
 * Replaces the original Postgres-only TEXT[] column so tests can run on H2
 * (which doesn't support array types) without losing data fidelity.
 *
 * Format: ["Java", "Spring Boot", "PostgreSQL"]
 *
 * Tradeoff vs Postgres TEXT[]:
 *   - Pro: portable across all JDBC databases
 *   - Con: no native array operators (e.g., array contains); use LIKE/JSON queries instead
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize list to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, LIST_TYPE);
        } catch (Exception e) {
            // Backward compat: old rows may have Postgres array format like {Java,Spring}
            // Fall back to parsing as comma-separated
            if (dbData.startsWith("{") && dbData.endsWith("}")) {
                String inner = dbData.substring(1, dbData.length() - 1);
                if (inner.isEmpty()) return new ArrayList<>();
                List<String> result = new ArrayList<>();
                for (String item : inner.split(",")) {
                    String trimmed = item.trim();
                    if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                        trimmed = trimmed.substring(1, trimmed.length() - 1);
                    }
                    if (!trimmed.isEmpty()) result.add(trimmed);
                }
                return result;
            }
            return new ArrayList<>();
        }
    }
}
