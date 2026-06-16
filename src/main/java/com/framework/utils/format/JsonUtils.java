package com.framework.utils.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** JSON data provider utility. */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private JsonUtils() {}

    /** Read JSON file into a Map. */
    public static Map<String, Object> readJson(String filePath) {
        try {
            return MAPPER.readValue(new File(filePath),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON: " + filePath, e);
        }
    }

    /** Read JSON from classpath. */
    public static Map<String, Object> readJsonFromClasspath(String path) {
        try (InputStream is = JsonUtils.class.getResourceAsStream(path)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + path);
            return MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON from classpath: " + path, e);
        }
    }

    /** Read JSON array into a list of maps. */
    public static List<Map<String, Object>> readJsonArray(String filePath) {
        try {
            return MAPPER.readValue(new File(filePath),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON array: " + filePath, e);
        }
    }

    /** Write object to JSON file. */
    public static void writeJson(String filePath, Object data) {
        try {
            MAPPER.writeValue(new File(filePath), data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write JSON: " + filePath, e);
        }
    }

    /** Convert object to JSON string. */
    public static String toJson(Object data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /** Convert JSON string to Map. */
    public static Map<String, Object> fromJson(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON string", e);
        }
    }
}
