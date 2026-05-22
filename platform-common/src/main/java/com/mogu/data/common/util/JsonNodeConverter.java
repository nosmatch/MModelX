package com.mogu.data.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

/**
 * JPA 转换器：将 JsonNode 与 PostgreSQL jsonb 互相转换
 *
 * 使用 PGobject 包装，确保 JDBC 驱动以 jsonb 类型传参，避免 varchar 类型不匹配错误。
 */
@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(attribute.toString());
            return pgo;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to convert JsonNode to PGobject", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        String json = dbData instanceof PGobject ? ((PGobject) dbData).getValue() : dbData.toString();
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON: " + json, e);
        }
    }
}
