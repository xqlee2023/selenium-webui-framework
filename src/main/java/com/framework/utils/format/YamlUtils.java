package com.framework.utils.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/** YAML data provider utility. */
public final class YamlUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private YamlUtils() {}

    /** Read YAML file into a Map. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> readYaml(String filePath) {
        try {
            return MAPPER.readValue(new File(filePath), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read YAML: " + filePath, e);
        }
    }

    /** Read YAML from classpath. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> readYamlFromClasspath(String path) {
        try (InputStream is = YamlUtils.class.getResourceAsStream(path)) {
            if (is == null) throw new IllegalArgumentException("Resource not found: " + path);
            return MAPPER.readValue(is, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read YAML from classpath: " + path, e);
        }
    }

    /** Read YAML into a specific type. */
    public static <T> T readYamlAs(String filePath, Class<T> clazz) {
        try {
            return MAPPER.readValue(new File(filePath), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read YAML as " + clazz.getSimpleName(), e);
        }
    }
}
