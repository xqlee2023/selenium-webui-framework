package com.framework.utils.data;

import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;
import java.util.*;
import com.framework.utils.format.CsvUtils;
import com.framework.utils.format.ExcelUtils;
import com.framework.utils.format.JsonUtils;
import com.framework.utils.format.YamlUtils;

/**
 * Central TestNG DataProvider factory.
 * Supports Excel, JSON, YAML, CSV, and inline data sources.
 *
 * Usage in test:
 *   @Test(dataProvider = "excel", dataProviderClass = DataProviderHelper.class)
 *   @DataFile(path = "src/test/resources/testdata/login.xlsx", sheet = "valid_users")
 */
public class DataProviderHelper {

    // Thread-local for parallel safety
    private static final ThreadLocal<Map<String, String>> currentRow = new ThreadLocal<>();

    /** Get current data row (for attaching to reports). */
    public static Map<String, String> getCurrentRow() {
        return currentRow.get();
    }

    // ========== Data Providers ==========

    @DataProvider(name = "excel", parallel = true)
    public static Iterator<Object[]> excelDataProvider(Method method) {
        DataFile annotation = method.getAnnotation(DataFile.class);
        if (annotation == null) {
            throw new IllegalArgumentException("@DataFile annotation required on " + method.getName());
        }
        String path = annotation.path();
        String sheet = annotation.sheet();
        List<Map<String, String>> data = ExcelUtils.readSheet(path, sheet);
        return toIterator(data);
    }

    @DataProvider(name = "json", parallel = true)
    public static Iterator<Object[]> jsonDataProvider(Method method) {
        DataFile annotation = method.getAnnotation(DataFile.class);
        if (annotation == null) {
            throw new IllegalArgumentException("@DataFile annotation required on " + method.getName());
        }
        String path = annotation.path();
        String keyPath = annotation.key();
        List<Map<String, Object>> data;

        if (keyPath.isEmpty()) {
            data = JsonUtils.readJsonArray(path);
        } else {
            Map<String, Object> root = JsonUtils.readJson(path);
            data = resolveJsonPath(root, keyPath);
        }

        return toObjectIterator(data);
    }

    @DataProvider(name = "yaml", parallel = true)
    public static Iterator<Object[]> yamlDataProvider(Method method) {
        DataFile annotation = method.getAnnotation(DataFile.class);
        if (annotation == null) {
            throw new IllegalArgumentException("@DataFile annotation required on " + method.getName());
        }
        String path = annotation.path();
        String keyPath = annotation.key();
        Map<String, Object> root = YamlUtils.readYaml(path);

        List<Map<String, Object>> data;
        if (keyPath.isEmpty()) {
            // Assume root is an array-like structure
            data = new ArrayList<>();
            data.add(root);
        } else {
            data = resolveJsonPath(root, keyPath);
        }

        return toObjectIterator(data);
    }

    @DataProvider(name = "csv", parallel = true)
    public static Iterator<Object[]> csvDataProvider(Method method) {
        DataFile annotation = method.getAnnotation(DataFile.class);
        if (annotation == null) {
            throw new IllegalArgumentException("@DataFile annotation required on " + method.getName());
        }
        List<Map<String, String>> data = CsvUtils.readCsv(annotation.path());
        return toIterator(data);
    }

    @DataProvider(name = "hardcoded", parallel = true)
    public static Iterator<Object[]> hardcodedDataProvider() {
        List<Object[]> data = new ArrayList<>();
        data.add(new Object[]{"admin", "password123", "valid user"});
        data.add(new Object[]{"guest", "guest123", "limited user"});
        return data.iterator();
    }

    // ========== Helpers ==========

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> resolveJsonPath(Map<String, Object> root, String keyPath) {
        String[] keys = keyPath.split("\\.");
        Object current = root;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            }
            if (current == null) break;
        }
        if (current instanceof List) {
            return (List<Map<String, Object>>) current;
        }
        return Collections.singletonList((Map<String, Object>) current);
    }

    private static Iterator<Object[]> toIterator(List<Map<String, String>> data) {
        List<Object[]> iteratorData = new ArrayList<>();
        for (Map<String, String> row : data) {
            iteratorData.add(new Object[]{row});
        }
        return iteratorData.iterator();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Iterator<Object[]> toObjectIterator(List<Map<String, Object>> data) {
        List<Object[]> iteratorData = new ArrayList<>();
        for (Map<String, Object> row : data) {
            iteratorData.add(new Object[]{row});
        }
        return iteratorData.iterator();
    }
}
