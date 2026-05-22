package com.mogu.data.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * JPA 转换器：将 JsonNode 与 JSON 字符串互相转换
 *
 * 用于在实体中使用 JsonNode 类型，但存储为 PostgreSQL 的 jsonb 列
 */
@Converter(autoApply = true)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString();
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(dbData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON: " + dbData, e);
        }
    }
}
