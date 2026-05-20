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
    @DisplayName("Verify extracting global prompt wrapping a 'data' array and conditionally injecting it")
    public void testAiPromptAndDataIsolation()
    {
        String yamlContent = 
            "prompt: |\n" +
            "  Verify that the user login succeeds\n" +
            "  Ensure to click submit\n" +
            "data:\n" +
            "  - testId: aiCase1\n" +
            "    user: test@example.com\n" +
            "  - testId: aiCase2\n" +
            "    user: dev@example.com\n" +
            "    prompt: This dataset overrides the global prompt";
            
        List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
        
        assertEquals(2, data.size());
        
        // Assert iteration 1 matches including the global implicitly injected 'prompt' var
        assertEquals("aiCase1", data.get(0).get("testId"));
        assertEquals("test@example.com", data.get(0).get("user"));
        assertTrue(data.get(0).get("prompt").contains("Verify that the user login succeeds"));
        
        // Assert iteration 2 isolates iteration mapping but retains local prompt override
        assertEquals("aiCase2", data.get(1).get("testId"));
        assertEquals("dev@example.com", data.get(1).get("user"));
        assertTrue(data.get(1).get("prompt").contains("This dataset overrides the global prompt"));
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
}
