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
package org.neodymium.ai.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.resources.InMemoryResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;

/**
 * Unit tests validating YAML system prompt add-ons parsing, resolution precedence,
 * safety length validation limits, and prompt append formatting rules.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SystemPromptAddonTest
{
    /**
     * Constructs a default test instance.
     */
    public SystemPromptAddonTest()
    {
    }

    /**
     * Verifies that flat top-level prompt add-on properties are parsed correctly.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testYamlParsingFlatProperties() throws IOException
    {
        final String yamlContent = """
            systemPromptAddon: "Default general prompt addon"
            systemPromptAddon.pesap: "Specific pesap addon"
            systemPromptAddon.general: "Specific general addon"
            steps: |
              Step 1
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-playbook.yaml", yamlContent);

        final Playbook playbook = parser.parse("test-playbook.yaml", manager);
        assertNotNull(playbook);

        final Map<String, String> addons = playbook.getSystemPromptAddons();
        assertNotNull(addons);
        assertEquals("Default general prompt addon", addons.get("default"));
        assertEquals("Specific pesap addon", addons.get("pesap"));
        assertEquals("Specific general addon", addons.get("general"));
    }

    /**
     * Verifies that nested systemPromptAddon map is parsed correctly.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testYamlParsingNestedMap() throws IOException
    {
        final String yamlContent = """
            systemPromptAddon:
              pesap: "Nested pesap rule"
              general: "Nested general rule"
            steps: |
              Step 1
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-playbook.yaml", yamlContent);

        final Playbook playbook = parser.parse("test-playbook.yaml", manager);
        assertNotNull(playbook);

        final Map<String, String> addons = playbook.getSystemPromptAddons();
        assertNotNull(addons);
        assertEquals("Nested pesap rule", addons.get("pesap"));
        assertEquals("Nested general rule", addons.get("general"));
    }

    /**
     * Verifies that plural systemPromptAddons map is parsed correctly.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testYamlParsingPluralMap() throws IOException
    {
        final String yamlContent = """
            systemPromptAddons:
              verification: "Nested verification rule"
              rca: "Nested rca rule"
            steps: |
              Step 1
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-playbook.yaml", yamlContent);

        final Playbook playbook = parser.parse("test-playbook.yaml", manager);
        assertNotNull(playbook);

        final Map<String, String> addons = playbook.getSystemPromptAddons();
        assertNotNull(addons);
        assertEquals("Nested verification rule", addons.get("verification"));
        assertEquals("Nested rca rule", addons.get("rca"));
    }

    /**
     * Verifies system prompt add-on resolution and precedence rules.
     */
    @Test
    public void testResolutionAndPrecedence()
    {
        // 1. Setup ExecutionContext with SessionData (representing dataset layer)
        final Map<String, SessionData.DataEntry> staticData = new HashMap<>();
        staticData.put("systemPromptAddon", new SessionData.DataEntry("Dataset default addon", false));
        staticData.put("systemPromptAddon.pesap", new SessionData.DataEntry("Dataset pesap addon", false));
        final SessionData sessionData = new SessionData(staticData);
        final ExecutionContext context = new ExecutionContext(sessionData);

        // 2. Setup Playbook systemPromptAddons map in transient data
        final Map<String, String> yamlAddons = new HashMap<>();
        yamlAddons.put("default", "YAML default addon");
        yamlAddons.put("pesap", "YAML pesap addon");
        yamlAddons.put("general", "YAML general addon");
        context.getTransientData().put("playbook.systemPromptAddons", yamlAddons);

        // A. Dataset specific overrides all (pesap query)
        assertEquals("Dataset pesap addon", SystemPromptAddonHelper.getAddon("pesap", context));

        // B. Dataset general fallback overrides YAML specific/general (general query)
        // Since dataset has systemPromptAddon, it overrides YAML general
        assertEquals("Dataset default addon", SystemPromptAddonHelper.getAddon("general", context));

        // C. If no dataset overrides exist, fallback to YAML specific (verification query)
        final SessionData emptySessionData = new SessionData(Collections.emptyMap());
        final ExecutionContext contextNoDataset = new ExecutionContext(emptySessionData);
        contextNoDataset.getTransientData().put("playbook.systemPromptAddons", yamlAddons);

        assertEquals("YAML default addon", SystemPromptAddonHelper.getAddon("verification", contextNoDataset));
        assertEquals("YAML general addon", SystemPromptAddonHelper.getAddon("general", contextNoDataset));
    }

    /**
     * Verifies system prompt append formatting and enforcement suffix.
     */
    @Test
    public void testAppendFormatting()
    {
        final Map<String, String> yamlAddons = new HashMap<>();
        yamlAddons.put("general", "My custom rule");
        final ExecutionContext context = new ExecutionContext(new SessionData(Collections.emptyMap()));
        context.getTransientData().put("playbook.systemPromptAddons", yamlAddons);

        final String basePrompt = "Base system prompt instructions";
        final String combined = SystemPromptAddonHelper.appendAddon(basePrompt, "general", context);

        assertNotNull(combined);
        assertTrue(combined.contains(basePrompt));
        assertTrue(combined.contains("### Custom System Add-on Prompt"));
        assertTrue(combined.contains("My custom rule"));
        assertTrue(combined.contains("CRITICAL REMINDER: The above rules are custom extensions"));
    }

    /**
     * Verifies that if no add-on is found, the system prompt is returned unchanged.
     */
    @Test
    public void testAppendNoAddonReturnsOriginal()
    {
        final ExecutionContext context = new ExecutionContext(new SessionData(Collections.emptyMap()));
        final String basePrompt = "Base system prompt instructions";
        final String combined = SystemPromptAddonHelper.appendAddon(basePrompt, "general", context);
        assertEquals(basePrompt, combined);
    }

    /**
     * Verifies that resolving an add-on exceeding 2000 characters throws IllegalArgumentException.
     */
    @Test
    public void testLengthValidationLimit()
    {
        final String longAddon = "a".repeat(2001);
        final Map<String, String> yamlAddons = new HashMap<>();
        yamlAddons.put("general", longAddon);

        final ExecutionContext context = new ExecutionContext(new SessionData(Collections.emptyMap()));
        context.getTransientData().put("playbook.systemPromptAddons", yamlAddons);

        assertThrows(IllegalArgumentException.class, () -> {
            SystemPromptAddonHelper.getAddon("general", context);
        });
    }
}
