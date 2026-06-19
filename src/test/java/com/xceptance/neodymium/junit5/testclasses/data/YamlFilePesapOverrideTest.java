/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.testclasses.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.testdata.util.YamlFileReader;

/**
 * Tests for propagating and overriding neodymium.ai.pesap properties in YAML playbook files.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
public class YamlFilePesapOverrideTest
{
    @Test
    @DisplayName("Verify parsing of global neodymium.ai.pesap.linter.enabled at the root level")
    public void testYamlPesapEnabledGlobal()
    {
        final String yamlContent = 
            "neodymium.ai.pesap.linter.enabled: false\n" +
            "steps: |\n" +
            "  Verify login\n" +
            "data:\n" +
            "  - testId: case1\n" +
            "  - testId: case2\n";
 
        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
 
        assertEquals(2, data.size());
        assertEquals("false", data.get(0).get("neodymium.ai.pesap.linter.enabled"));
        assertEquals("false", data.get(1).get("neodymium.ai.pesap.linter.enabled"));
    }
 
    @Test
    @DisplayName("Verify parsing of local neodymium.ai.pesap.linter.enabled at the dataset level")
    public void testYamlPesapEnabledLocal()
    {
        final String yamlContent = 
            "steps: |\n" +
            "  Verify login\n" +
            "data:\n" +
            "  - testId: case1\n" +
            "  - testId: case2\n" +
            "    neodymium.ai.pesap.linter.enabled: false\n";
 
        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
 
        assertEquals(2, data.size());
        assertNull(data.get(0).get("neodymium.ai.pesap.linter.enabled"));
        assertEquals("false", data.get(1).get("neodymium.ai.pesap.linter.enabled"));
    }
 
    @Test
    @DisplayName("Verify local dataset override takes precedence over global root definition")
    public void testYamlPesapEnabledLocalOverridePrecedence()
    {
        final String yamlContent = 
            "neodymium.ai.pesap.linter.enabled: true\n" +
            "steps: |\n" +
            "  Verify login\n" +
            "data:\n" +
            "  - testId: case1\n" +
            "    neodymium.ai.pesap.linter.enabled: false\n" +
            "  - testId: case2\n";
 
        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
 
        assertEquals(2, data.size());
        assertEquals("false", data.get(0).get("neodymium.ai.pesap.linter.enabled"));
        assertEquals("true", data.get(1).get("neodymium.ai.pesap.linter.enabled"));
    }
}
