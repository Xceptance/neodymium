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
import org.neodymium.ai.model.PlaybookStep;
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

    @Test
    public void testParsePlaybookWithBeforeStepsAndAfter() throws IOException
    {
        final String yamlContent = """
            before: |
              Open https://www.example.com
              Accept cookies
            steps: |
              Search for product
              Add to cart
            after: |
              Clear cookies
              Close banner
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("lifecycle-playbook.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("lifecycle-playbook.yaml", manager);

        assertNotNull(playbook);
        assertEquals(6, playbook.getSteps().size());
        assertEquals("Open https://www.example.com", playbook.getSteps().get(0).getInstruction());
        assertEquals("Accept cookies", playbook.getSteps().get(1).getInstruction());
        assertEquals("Search for product", playbook.getSteps().get(2).getInstruction());
        assertEquals("Add to cart", playbook.getSteps().get(3).getInstruction());
        assertEquals("Clear cookies", playbook.getSteps().get(4).getInstruction());
        assertEquals("Close banner", playbook.getSteps().get(5).getInstruction());
    }

    @Test
    public void testParsePlaybookWithAllBeforeAliases() throws IOException
    {
        final String yamlContent = """
            before: |
              Step before
            beforeEach: |
              Step beforeEach
            _beforeEach: |
              Step _beforeEach
            _beforeAll: |
              Step _beforeAll
            steps: |
              Main step
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("before-aliases.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("before-aliases.yaml", manager);

        assertNotNull(playbook);
        assertEquals(5, playbook.getSteps().size());
        assertEquals("Step before", playbook.getSteps().get(0).getInstruction());
        assertEquals("Step beforeEach", playbook.getSteps().get(1).getInstruction());
        assertEquals("Step _beforeEach", playbook.getSteps().get(2).getInstruction());
        assertEquals("Step _beforeAll", playbook.getSteps().get(3).getInstruction());
        assertEquals("Main step", playbook.getSteps().get(4).getInstruction());
    }

    @Test
    public void testParsePlaybookWithAllAfterAliases() throws IOException
    {
        final String yamlContent = """
            steps: |
              Main step
            after: |
              Step after
            afterEach: |
              Step afterEach
            _afterEach: |
              Step _afterEach
            _afterAll: |
              Step _afterAll
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("after-aliases.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("after-aliases.yaml", manager);

        assertNotNull(playbook);
        assertEquals(5, playbook.getSteps().size());
        assertEquals("Main step", playbook.getSteps().get(0).getInstruction());
        assertEquals("Step after", playbook.getSteps().get(1).getInstruction());
        assertEquals("Step afterEach", playbook.getSteps().get(2).getInstruction());
        assertEquals("Step _afterEach", playbook.getSteps().get(3).getInstruction());
        assertEquals("Step _afterAll", playbook.getSteps().get(4).getInstruction());
    }

    @Test
    public void testParsePlaybookWithStructuredActionsInBeforeAndAfter() throws IOException
    {
        final String yamlContent = """
            before:
              - instruction: "Open home page"
                actions:
                  - type: "NAVIGATE"
                    target: "https://example.com"
            steps:
              - instruction: "Click login button"
                actions:
                  - type: "CLICK"
                    target: "#login-btn"
            after:
              - instruction: "Clear session"
                actions:
                  - type: "CLEAR_COOKIES"
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("structured-lifecycle.yaml", yamlContent);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("structured-lifecycle.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size());
        assertEquals("Open home page", playbook.getSteps().get(0).getInstruction());
        assertEquals(1, playbook.getSteps().get(0).getActions().size());
        assertEquals("NAVIGATE", playbook.getSteps().get(0).getActions().get(0).getType());

        assertEquals("Click login button", playbook.getSteps().get(1).getInstruction());
        assertEquals(1, playbook.getSteps().get(1).getActions().size());
        assertEquals("CLICK", playbook.getSteps().get(1).getActions().get(0).getType());

        assertEquals("Clear session", playbook.getSteps().get(2).getInstruction());
        assertEquals(1, playbook.getSteps().get(2).getActions().size());
        assertEquals("CLEAR_COOKIES", playbook.getSteps().get(2).getActions().get(0).getType());
    }

    @Test
    public void testParsePlaybookWithIncludesInBeforeAndAfter() throws IOException
    {
        final String mainYaml = """
            before: |
              _include: setup.yaml
            steps: |
              Main step
            after: |
              _include: teardown.yaml
            """;

        final String setupYaml = """
            steps: |
              Setup step 1
              Setup step 2
            """;

        final String teardownYaml = """
            steps: |
              Teardown step 1
            """;

        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("main.yaml", mainYaml);
        manager.write("setup.yaml", setupYaml);
        manager.write("teardown.yaml", teardownYaml);

        final YamlPlaybookParser parser = new YamlPlaybookParser();
        final Playbook playbook = parser.parse("main.yaml", manager);

        assertNotNull(playbook);
        assertEquals(3, playbook.getSteps().size());

        final PlaybookStep beforeInclude = playbook.getSteps().get(0);
        assertEquals("_include: setup.yaml", beforeInclude.getInstruction());
        assertEquals(2, beforeInclude.getSubSteps().size());
        assertEquals("Setup step 1", beforeInclude.getSubSteps().get(0).getInstruction());
        assertEquals("Setup step 2", beforeInclude.getSubSteps().get(1).getInstruction());

        assertEquals("Main step", playbook.getSteps().get(1).getInstruction());

        final PlaybookStep afterInclude = playbook.getSteps().get(2);
        assertEquals("_include: teardown.yaml", afterInclude.getInstruction());
        assertEquals(1, afterInclude.getSubSteps().size());
        assertEquals("Teardown step 1", afterInclude.getSubSteps().get(0).getInstruction());
    }
}
