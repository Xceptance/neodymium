package com.xceptance.neodymium.junit5.testclasses.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.testdata.util.YamlFileReader;

public class YamlFileReaderTest
{
    @Test
    @DisplayName("Verify parsing of basic YAML lists mimicking standard JSON arrays")
    public void testBasicYamlArray()
    {
        String yamlContent = 
            "- testId: arrayCase1\n" +
            "  value: hello\n" +
            "- testId: arrayCase2\n" +
            "  value: world";
            
        List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
        
        assertEquals(2, data.size());
        assertEquals("arrayCase1", data.get(0).get("testId"));
        assertEquals("hello", data.get(0).get("value"));
        assertEquals("arrayCase2", data.get(1).get("testId"));
        assertEquals("world", data.get(1).get("value"));
    }

    @Test
    @DisplayName("Verify extracting global steps wrapping a 'data' array and conditionally injecting it")
    public void testAiStepsAndDataIsolation()
    {
        final String yamlContent = 
            "steps: |\n" +
            "  Verify that the user login succeeds\n" +
            "  Ensure to click submit\n" +
            "data:\n" +
            "  - testId: aiCase1\n" +
            "    user: test@example.com\n" +
            "  - testId: aiCase2\n" +
            "    user: dev@example.com\n" +
            "    steps: This dataset overrides the global steps";
            
        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
        
        assertEquals(2, data.size());
        
        // Assert iteration 1 matches including the global implicitly injected 'steps' var
        assertEquals("aiCase1", data.get(0).get("testId"));
        assertEquals("test@example.com", data.get(0).get("user"));
        assertTrue(data.get(0).get("steps").contains("Verify that the user login succeeds"));
        
        // Assert iteration 2 isolates iteration mapping but retains local steps override
        assertEquals("aiCase2", data.get(1).get("testId"));
        assertEquals("dev@example.com", data.get(1).get("user"));
        assertTrue(data.get(1).get("steps").contains("This dataset overrides the global steps"));
    }

    @Test
    @DisplayName("Verify complex object conversion back to JSON strings for @DataItem parity")
    public void testComplexVariableBindings()
    {
        String yamlContent = 
            "- testId: complexBinding\n" +
            "  user:\n" +
            "    firstName: Max\n" +
            "    lastName: Mustermann";
            
        List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
        
        assertEquals(1, data.size());
        assertEquals("complexBinding", data.get(0).get("testId"));
        assertNotNull(data.get(0).get("user"));
        
        // Complex nesting natively gets JSON mapped precisely back into Neodymium's POJO expectation layer
        assertTrue(data.get(0).get("user").contains("\"firstName\":\"Max\""));
        assertTrue(data.get(0).get("user").contains("\"lastName\":\"Mustermann\""));
    }

    @Test
    @DisplayName("Verify parsing of step line numbers for both global and local steps")
    public void testYamlStepLineNumbers()
    {
        final String yamlContent = 
            "# Line 1\n" +
            "steps: |\n" + // Line 2: start mark should be line 2 (0-indexed line 1)
            "  Verify login\n" + // Line 3 (index 0)
            "  # A comment inside steps\n" + // Line 4
            "  Click submit\n" + // Line 5 (index 1)
            "data:\n" + // Line 6
            "  - testId: case1\n" + // Line 7
            "  - testId: case2\n" + // Line 8
            "    steps: |\n" + // Line 9: start mark for local steps
            "      Local action 1\n" + // Line 10 (index 0)
            "      Local action 2"; // Line 11 (index 1)

        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, data.size());

        // Check global step line numbers (Case 1)
        final String globalStepLinesJson = data.get(0).get("neodymium.stepLineNumbers");
        assertNotNull(globalStepLinesJson);
        final List<Double> globalStepLines = new com.google.gson.Gson().fromJson(globalStepLinesJson, new com.google.gson.reflect.TypeToken<List<Double>>(){}.getType());
        assertEquals(2, globalStepLines.size());
        assertEquals(3, globalStepLines.get(0).intValue());
        assertEquals(5, globalStepLines.get(1).intValue());

        // Check local step line numbers (Case 2)
        final String localStepLinesJson = data.get(1).get("neodymium.stepLineNumbers");
        assertNotNull(localStepLinesJson);
        final List<Double> localStepLines = new com.google.gson.Gson().fromJson(localStepLinesJson, new com.google.gson.reflect.TypeToken<List<Double>>(){}.getType());
        assertEquals(2, localStepLines.size());
        assertEquals(10, localStepLines.get(0).intValue());
        assertEquals(11, localStepLines.get(1).intValue());
    }
}

