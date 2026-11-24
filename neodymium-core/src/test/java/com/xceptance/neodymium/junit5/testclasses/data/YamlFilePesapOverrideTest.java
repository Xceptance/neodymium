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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.neodymium.common.testdata.util.YamlFileReader;

/**
 * Tests for propagating and overriding neodymium.ai.pesap properties in YAML playbook files.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
public class YamlFilePesapOverrideTest
{
    @Test
    @DisplayName("Verify parsing of global neodymium.ai.linter.enabled at the root level")
    public void testYamlPesapEnabledGlobal()
    {
        final String yamlContent = 
            "neodymium.ai.linter.enabled: false\n" +
            "steps: |\n" +
            "  Verify login\n" +
            "data:\n" +
            "  - testId: case1\n" +
            "  - testId: case2\n";
 
        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
 
        assertEquals(2, data.size());
        assertEquals("false", data.get(0).get("neodymium.ai.linter.enabled"));
        assertEquals("false", data.get(1).get("neodymium.ai.linter.enabled"));
    }
 
    @Test
    @DisplayName("Verify parsing of local neodymium.ai.linter.enabled at the dataset level")
    public void testYamlPesapEnabledLocal()
    {
        final String yamlContent = 
            "steps: |\n" +
            "  Verify login\n" +
            "data:\n" +
            "  - testId: case1\n" +
            "  - testId: case2\n" +
            "    neodymium.ai.linter.enabled: false\n";
 
        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
 
        assertEquals(2, data.size());
        assertNull(data.get(0).get("neodymium.ai.linter.enabled"));
        assertEquals("false", data.get(1).get("neodymium.ai.linter.enabled"));
    }
 
    @Test
    @DisplayName("Verify local dataset override takes precedence over global root definition")
    public void testYamlPesapEnabledLocalOverridePrecedence()
    {
        final String yamlContent = 
            "neodymium.ai.linter.enabled: true\n" +
            "steps: |\n" +
            "  Verify login\n" +
            "data:\n" +
            "  - testId: case1\n" +
            "    neodymium.ai.linter.enabled: false\n" +
            "  - testId: case2\n";
 
        final List<Map<String, String>> data = YamlFileReader.readFile(new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8)));
 
        assertEquals(2, data.size());
        assertEquals("false", data.get(0).get("neodymium.ai.linter.enabled"));
        assertEquals("true", data.get(1).get("neodymium.ai.linter.enabled"));
    }
}
