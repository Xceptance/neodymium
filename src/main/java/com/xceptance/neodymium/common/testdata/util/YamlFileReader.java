package com.xceptance.neodymium.common.testdata.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class YamlFileReader {
    private final static Gson GSON = new GsonBuilder().serializeNulls().create();

    public static List<Map<String, String>> readFile(final InputStream inputStream)
    {
        final List<Map<String, String>> resultData = new LinkedList<>();
        final Yaml yaml = new Yaml();

        try
        {
            final byte[] yamlBytes;
            try (final InputStream is = inputStream)
            {
                yamlBytes = is.readAllBytes();
            }

            final Object data = yaml.load(new ByteArrayInputStream(yamlBytes));

            if (data == null)
            {
                return resultData;
            }

            final String yamlStr = new String(yamlBytes, StandardCharsets.UTF_8);
            final List<String> yamlLines = yamlStr.lines().toList();

            String globalStepsLinesJson = null;
            final List<String> localStepsLinesJsons = new ArrayList<>();

            try
            {
                final Node rootNode = yaml.compose(new InputStreamReader(new ByteArrayInputStream(yamlBytes), StandardCharsets.UTF_8));
                if (rootNode instanceof MappingNode rootMapping)
                {
                    final Node globalStepsNode = getMappingValue(rootMapping, "steps");
                    if (globalStepsNode instanceof ScalarNode stepsScalar)
                    {
                        globalStepsLinesJson = parseStepLineNumbers(stepsScalar, yamlLines);
                    }

                    final Node dataNode = getMappingValue(rootMapping, "data");
                    if (dataNode instanceof SequenceNode dataSequence)
                    {
                        for (final Node itemNode : dataSequence.getValue())
                        {
                            if (itemNode instanceof MappingNode itemMapping)
                            {
                                final Node localStepsNode = getMappingValue(itemMapping, "steps");
                                if (localStepsNode instanceof ScalarNode localStepsScalar)
                                {
                                    localStepsLinesJsons.add(parseStepLineNumbers(localStepsScalar, yamlLines));
                                }
                                else
                                {
                                    localStepsLinesJsons.add(null);
                                }
                            }
                            else
                            {
                                localStepsLinesJsons.add(null);
                            }
                        }
                    }
                }
                else if (rootNode instanceof SequenceNode rootSequence)
                {
                    for (final Node itemNode : rootSequence.getValue())
                    {
                        if (itemNode instanceof MappingNode itemMapping)
                        {
                            final Node localStepsNode = getMappingValue(itemMapping, "steps");
                            if (localStepsNode instanceof ScalarNode localStepsScalar)
                            {
                                localStepsLinesJsons.add(parseStepLineNumbers(localStepsScalar, yamlLines));
                            }
                            else
                            {
                                localStepsLinesJsons.add(null);
                            }
                        }
                        else
                        {
                            localStepsLinesJsons.add(null);
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                // Soft fail Node parsing so normal load remains fully functional
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
                int datasetIdx = 0;
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

                        // Determine and inject step line numbers for this dataset
                        String stepLineNumbersJson = null;
                        if (datasetIdx < localStepsLinesJsons.size() && localStepsLinesJsons.get(datasetIdx) != null)
                        {
                            stepLineNumbersJson = localStepsLinesJsons.get(datasetIdx);
                        }
                        else
                        {
                            stepLineNumbersJson = globalStepsLinesJson;
                        }

                        if (stepLineNumbersJson != null)
                        {
                            newDataSet.put("neodymium.stepLineNumbers", stepLineNumbersJson);
                        }

                        resultData.add(newDataSet);
                        datasetIdx++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing YAML file", e);
        }

        return resultData;
    }

    /**
     * Finds the value node associated with a specific key in a mapping node.
     *
     * @param mappingNode the mapping node to search
     * @param key         the key to search for
     * @return the value node, or null if not found
     */
    private static Node getMappingValue(final MappingNode mappingNode, final String key)
    {
        for (final NodeTuple tuple : mappingNode.getValue())
        {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode && key.equals(keyNode.getValue()))
            {
                return tuple.getValueNode();
            }
        }
        return null;
    }

    /**
     * Parses the line numbers of steps inside a steps scalar node.
     *
     * @param stepsNode the steps scalar node
     * @param yamlLines the lines of the YAML document
     * @return the serialized JSON list of step line numbers
     */
    private static String parseStepLineNumbers(final ScalarNode stepsNode, final List<String> yamlLines)
    {
        final List<Integer> lineNumbers = new LinkedList<>();
        final String stepsContent = stepsNode.getValue();
        if (stepsContent == null)
        {
            return GSON.toJson(lineNumbers);
        }

        final int startLineIdx = stepsNode.getStartMark().getLine();
        int currentY = startLineIdx;

        final String[] contentLines = stepsContent.split("\\r?\\n", -1);

        for (final String line : contentLines)
        {
            while (currentY < yamlLines.size())
            {
                final String yamlLine = yamlLines.get(currentY);
                if (matchesLine(yamlLine, line))
                {
                    final String trimmedLine = line.strip();
                    if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#") && !trimmedLine.startsWith("//"))
                    {
                        lineNumbers.add(currentY + 1);
                    }
                    break;
                }
                currentY++;
            }
            currentY++;
        }
        return GSON.toJson(lineNumbers);
    }

    /**
     * Helper to check if a YAML line matches the content line.
     *
     * @param yamlLine    the line from the YAML file
     * @param contentLine the line from the steps scalar value
     * @return true if they match, false otherwise
     */
    private static boolean matchesLine(final String yamlLine, final String contentLine)
    {
        final String trimmedContent = contentLine.strip();
        final String trimmedYaml = yamlLine.strip();
        if (trimmedContent.isEmpty())
        {
            return trimmedYaml.isEmpty() || trimmedYaml.startsWith("#") || trimmedYaml.startsWith("//");
        }
        if (trimmedContent.startsWith("#") || trimmedContent.startsWith("//"))
        {
            return trimmedYaml.contains(trimmedContent) || trimmedYaml.startsWith("#") || trimmedYaml.startsWith("//");
        }
        return trimmedYaml.contains(trimmedContent);
    }

    public static List<Map<String, String>> readFile(File file) {
        try {
            return readFile(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + file.getAbsolutePath(), e);
        }
    }
}
