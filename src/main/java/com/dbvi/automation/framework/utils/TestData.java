package com.dbvi.automation.framework.utils;

import com.dbvi.automation.framework.config.FrameworkProperties;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * TestData utility class. Loads environment-specific test data from YAML files and provides
 * dot-notation lookup to fetch nested properties.
 *
 * <p>Supports both standard "testdata.yaml" and loading of custom files dynamically.
 */
public class TestData {
    private static final Yaml yaml = new Yaml();
    private static Map<String, Object> defaultDataMap = null;
    private static final Map<String, Map<String, Object>> customDataMaps = new HashMap<>();

    /** Helper to load default "testdata.yaml" for the active environment. */
    private static synchronized void loadDefaultData() {
        if (defaultDataMap == null) {
            defaultDataMap = loadYamlFile("testdata.yaml");
        }
    }

    /** Loads a specific YAML file from the active environment folder and caches it. */
    public static synchronized void loadCustomFile(String fileName) {
        if (!customDataMaps.containsKey(fileName)) {
            Map<String, Object> data = loadYamlFile(fileName);
            customDataMaps.put(fileName, data);
        }
    }

    /** Internal helper to load and parse a YAML file from the classpath. */
    private static Map<String, Object> loadYamlFile(String fileName) {
        String project = FrameworkProperties.getProjectName();
        String env = FrameworkProperties.getEnvironment();
        String resourcePath = "env/" + project + "/" + env + "/" + fileName;

        try (InputStream inputStream =
                TestData.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException(
                        "Test data file not found on classpath: " + resourcePath);
            }
            Map<String, Object> parsed = yaml.load(inputStream);
            return parsed != null ? parsed : new HashMap<>();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data from " + resourcePath, e);
        }
    }

    /**
     * Retrieves a property using dot-notation from the default testdata.yaml. e.g.,
     * TestData.get("user.username")
     */
    public static Object get(String keyPath) {
        loadDefaultData();
        return getFromMap(defaultDataMap, keyPath);
    }

    /**
     * Retrieves a typed property using dot-notation from the default testdata.yaml. e.g.,
     * TestData.get("search.expectedResults", Integer.class)
     */
    public static <T> T get(String keyPath, Class<T> clazz) {
        Object val = get(keyPath);
        return castValue(val, clazz);
    }

    /**
     * Retrieves a property using dot-notation from a specific custom YAML file. e.g.,
     * TestData.getCustom("otherfile.yaml", "key.path")
     */
    public static Object getCustom(String fileName, String keyPath) {
        loadCustomFile(fileName);
        Map<String, Object> customMap = customDataMaps.get(fileName);
        return getFromMap(customMap, keyPath);
    }

    /**
     * Retrieves a typed property using dot-notation from a specific custom YAML file. e.g.,
     * TestData.getCustom("otherfile.yaml", "key.path", String.class)
     */
    public static <T> T getCustom(String fileName, String keyPath, Class<T> clazz) {
        Object val = getCustom(fileName, keyPath);
        return castValue(val, clazz);
    }

    /** Dynamic traverser for nested Map hierarchies using dot-separated keys. */
    @SuppressWarnings("unchecked")
    private static Object getFromMap(Map<String, Object> map, String keyPath) {
        if (map == null || keyPath == null || keyPath.isEmpty()) {
            return null;
        }

        String[] keys = keyPath.split("\\.");
        Object current = map;

        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null;
            }
        }
        return current;
    }

    /** Safe caster supporting dynamic conversion between numerical types (Double/Integer/Long). */
    @SuppressWarnings("unchecked")
    private static <T> T castValue(Object val, Class<T> clazz) {
        if (val == null) {
            return null;
        }
        if (clazz.isInstance(val)) {
            return clazz.cast(val);
        }
        if (val instanceof Number) {
            Number num = (Number) val;
            if (clazz == Integer.class) {
                return (T) Integer.valueOf(num.intValue());
            }
            if (clazz == Double.class) {
                return (T) Double.valueOf(num.doubleValue());
            }
            if (clazz == Long.class) {
                return (T) Long.valueOf(num.longValue());
            }
        }
        return null;
    }
}
