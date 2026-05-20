package com.xceptance.neodymium.common.testdata.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class YamlFileReader {
    private final static Gson GSON = new GsonBuilder().serializeNulls().create();

    public static List<Map<String, String>> readFile(InputStream inputStream) {
        List<Map<String, String>> resultData = new LinkedList<>();
        Yaml yaml = new Yaml();

        try {
            Object data = yaml.load(inputStream);

            if (data == null) {
                return resultData;
            }

            List<?> iterationList = null;
            String steps = null;
            Object before = null;
            Object after = null;
            Object context = null;
            Object hints = null;

            // Scenario 1: The root is a map (complex structure, e.g., AI integration with
            // 'steps' and 'data')
            if (data instanceof Map) {
                Map<?, ?> rootMap = (Map<?, ?>) data;
                if (rootMap.containsKey("steps")) {
                    steps = String.valueOf(rootMap.get("steps"));
                }
                if (rootMap.containsKey("context")) {
                    context = rootMap.get("context");
                }
                if (rootMap.containsKey("systemContext")) {
                    context = rootMap.get("systemContext");
                }

                if (rootMap.containsKey("before")) {
                    before = rootMap.get("before");
                }
                if (rootMap.containsKey("after")) {
                    after = rootMap.get("after");
                }
                if (rootMap.containsKey("hints")) {
                    hints = rootMap.get("hints");
                }

                if (rootMap.containsKey("data") && rootMap.get("data") instanceof List) {
                    iterationList = (List<?>) rootMap.get("data");
                } else {
                    // Treat the map itself as a single iteration if it does not match the 'data'
                    // array format
                    iterationList = List.of(rootMap);
                }
            }
            // Scenario 2: The root is a list directly
            else if (data instanceof List) {
                iterationList = (List<?>) data;
            } else {
                throw new RuntimeException("YAML root must be a List or a Map containing a 'data' List.");
            }

            // Map each element into Map<String, String> preserving JSON-style serialization
            // for nesting
            if (iterationList != null) {
                for (Object item : iterationList) {
                    if (item instanceof Map) {
                        Map<?, ?> dataset = (Map<?, ?>) item;
                        Map<String, String> newDataSet = new HashMap<>();

                        for (Map.Entry<?, ?> entry : dataset.entrySet()) {
                            String key = entry.getKey().toString();
                            if ("systemContext".equals(key)) {
                                key = "context";
                            }
                            
                            Object value = entry.getValue();
                            if (value == null) {
                                newDataSet.put(key, null);
                            } else if (value instanceof Map || value instanceof List) {
                                // Serialize nested objects back to JSON string for consistent @DataItem parsing
                                newDataSet.put(key, GSON.toJson(value));
                            } else {
                                newDataSet.put(key, String.valueOf(value));
                            }
                        }

                        // Inject the AI global steps into every iteration context transparently if not
                        // locally overridden
                        if (steps != null && !newDataSet.containsKey("steps")) {
                            newDataSet.put("steps", steps);
                        }

                        if (before != null && !newDataSet.containsKey("before")) {
                            if (before instanceof List) {
                                newDataSet.put("before", GSON.toJson(before));
                            } else {
                                newDataSet.put("before", String.valueOf(before));
                            }
                        }

                        if (after != null && !newDataSet.containsKey("after")) {
                            if (after instanceof List) {
                                newDataSet.put("after", GSON.toJson(after));
                            } else {
                                newDataSet.put("after", String.valueOf(after));
                            }
                        }

                        if (context != null && !newDataSet.containsKey("context")) {
                            if (context instanceof List) {
                                newDataSet.put("context", GSON.toJson(context));
                            } else {
                                newDataSet.put("context", String.valueOf(context));
                            }
                        }

                        if (hints != null && hints instanceof Map) {
                            Map<?, ?> hintsMap = (Map<?, ?>) hints;
                            for (Map.Entry<?, ?> hintEntry : hintsMap.entrySet()) {
                                String hintKey = String.valueOf(hintEntry.getKey());
                                if (!newDataSet.containsKey(hintKey)) {
                                    newDataSet.put(hintKey, String.valueOf(hintEntry.getValue()));
                                }
                            }
                        }

                        resultData.add(newDataSet);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing YAML file", e);
        }

        return resultData;
    }

    public static List<Map<String, String>> readFile(File file) {
        try {
            return readFile(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath(), e);
        }
    }
}
