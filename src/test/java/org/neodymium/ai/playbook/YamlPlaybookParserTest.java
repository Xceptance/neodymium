/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.playbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.resources.InMemoryResourceManager;

/**
 * Unit tests for {@link YamlPlaybookParser}.
 * Validates parsing of step hierarchies and datasets from YAML string content,
 * including strict format validation and empty playbook rejection.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class YamlPlaybookParserTest
{
    @Test
    public void testParseValidYamlPlaybook() throws IOException
    {
        final String yamlContent = """
            steps:
              - instruction: "Open demo store homepage"
                actions:
                  - type: "NAVIGATE"
                    target: "http://localhost:8080"
              - instruction: "Click login button"
                actions:
                  - type: "CLICK"
                    target: "#login-btn"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("test-playbook.yaml", manager);

        assertNotNull(playbook, "Parsed playbook should not be null.");
        assertEquals(2, playbook.getSteps().size(), "Should parse 2 top-level playbook steps.");
        assertEquals("Open demo store homepage", playbook.getSteps().get(0).getInstruction());
        assertEquals(1, playbook.getSteps().get(0).getActions().size());
        assertEquals("NAVIGATE", playbook.getSteps().get(0).getActions().get(0).getType());
        assertEquals("Click login button", playbook.getSteps().get(1).getInstruction());
        assertEquals(1, playbook.getSteps().get(1).getActions().size());
        assertEquals("CLICK", playbook.getSteps().get(1).getActions().get(0).getType());
    }

    @Test
    public void testParseStringSteps() throws IOException
    {
        final String yamlContent = """
            steps:
              - "Open homepage"
              - "Click button"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-string-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("test-string-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(2, playbook.getSteps().size());
        assertEquals("Open homepage", playbook.getSteps().get(0).getInstruction());
        assertEquals("Click button", playbook.getSteps().get(1).getInstruction());
    }

    @Test
    public void testParseEmptyPlaybookThrowsException() throws Exception
    {
        final String yamlContent = """
            steps: []
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("empty-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            parser.parse("empty-playbook.yaml", manager);
        });

        assertEquals("Playbook cannot be empty: empty-playbook.yaml parsed to 0 executable steps.", ex.getMessage());
    }

    @Test
    public void testParseInvalidStepMapShapeThrowsException() throws Exception
    {
        final String yamlContent = """
            steps:
              - invalid_key: "some value"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("invalid-map-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            parser.parse("invalid-map-playbook.yaml", manager);
        });

        assertEquals("Invalid playbook step format in file: invalid-map-playbook.yaml. Expected string step, 'include' map, or 'instruction' map, but found map keys: [invalid_key]", ex.getMessage());
    }

    @Test
    public void testParseEmptyJsonPlaybookThrowsException() throws Exception
    {
        final String jsonContent = "[]";

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("empty-recording.json", jsonContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            parser.parse("empty-recording.json", manager);
        });

        assertEquals("Playbook cannot be empty: empty-recording.json parsed to 0 executable steps.", ex.getMessage());
    }

    @Test
    public void testParsePlaybookWithBeforeAndSteps() throws IOException
    {
        final String yamlContent = """
            before: |
              Open https://www.example.com
              Select English
            steps: |
              Click button
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("before-steps.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("before-steps.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size());
        assertEquals("Open https://www.example.com", playbook.getSteps().get(0).getInstruction());
        assertEquals("Select English", playbook.getSteps().get(1).getInstruction());
        assertEquals("Click button", playbook.getSteps().get(2).getInstruction());
    }
}
