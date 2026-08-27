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
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xceptance.neodymium.junit5.testclasses.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.common.testdata.util.YamlFileReader;

/**
 * Tests for test data variable interpolation where test data values reference other test data values,
 * both flat and nested properties.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class YamlTestDataInterpolationTest
{
    @Test
    @DisplayName("Verify flat test data referencing other test data fields")
    public final void testFlatTestDataInterpolation()
    {
        final String yamlContent = """
            data:
              - foo: "bar"
                foobar: "${foo}"
            """;

        final List<Map<String, String>> data = YamlFileReader.readFile(
            new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, data.size());
        final Map<String, String> row = data.get(0);
        assertEquals("bar", row.get("foo"));
        assertEquals("bar", row.get("foobar"));
    }

    @Test
    @DisplayName("Verify map-based flat test data referencing other test data fields without explicit list")
    public final void testMapBasedFlatTestDataInterpolation()
    {
        final String yamlContent = """
            data:
              foo: "bar"
              foobar: "${foo}"
            """;

        final List<Map<String, String>> data = YamlFileReader.readFile(
            new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, data.size());
        final Map<String, String> row = data.get(0);
        assertEquals("bar", row.get("foo"));
        assertEquals("bar", row.get("foobar"));
    }

    @Test
    @DisplayName("Verify nested object property interpolation in test data")
    public final void testNestedObjectPropertyInterpolation()
    {
        final String yamlContent = """
            data:
              - testId: test
                language: "Deutsch"
                phrase:
                  Part1: "Neo"
                  Part2: "dymium"
                searchPhrase: "${phrase.Part1}${phrase.Part2}"
            """;

        final List<Map<String, String>> data = YamlFileReader.readFile(
            new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, data.size());
        final Map<String, String> row = data.get(0);
        assertEquals("test", row.get("testId"));
        assertEquals("Deutsch", row.get("language"));
        assertNotNull(row.get("phrase"));
        assertEquals("Neodymium", row.get("searchPhrase"));
    }

    @Test
    @DisplayName("Verify multiple placeholders and mixed static text interpolation")
    public final void testMultiplePlaceholdersAndMixedText()
    {
        final String yamlContent = """
            data:
              - protocol: "https"
                host: "example.com"
                path: "api/v1"
                url: "${protocol}://${host}/${path}"
            """;

        final List<Map<String, String>> data = YamlFileReader.readFile(
            new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(1, data.size());
        final Map<String, String> row = data.get(0);
        assertEquals("https://example.com/api/v1", row.get("url"));
    }

    @Test
    @DisplayName("Verify multi-dataset scope isolation for interpolated variables")
    public final void testMultiDatasetScopeIsolation()
    {
        final String yamlContent = """
            data:
              - testId: case1
                env: "stage"
                endpoint: "https://${env}.example.com"
              - testId: case2
                env: "prod"
                endpoint: "https://${env}.example.com"
            """;

        final List<Map<String, String>> data = YamlFileReader.readFile(
            new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))
        );

        assertEquals(2, data.size());
        assertEquals("https://stage.example.com", data.get(0).get("endpoint"));
        assertEquals("https://prod.example.com", data.get(1).get("endpoint"));
    }
}
