package com.nutriscan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        try { return MAPPER.writeValueAsString(list == null ? List.of() : list); }
        catch (Exception e) { return "[]"; }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return MAPPER.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) { return new ArrayList<>(); }
    }
}