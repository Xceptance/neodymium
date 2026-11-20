/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
