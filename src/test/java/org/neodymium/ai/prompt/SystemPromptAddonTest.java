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
     * Verifies that flat canonical promptAddon properties are parsed correctly.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testYamlParsingCanonicalPromptAddonFlat() throws IOException
    {
        final String yamlContent = """
            promptAddon: "Default general prompt addon"
            promptAddon.pesap: "Specific pesap addon"
            promptAddon.general: "Specific general addon"
            steps: |
              Step 1
            """;

        final PlaybookParser parser = new YamlPlaybookParser();
        final InMemoryResourceManager manager = new InMemoryResourceManager();
        manager.write("test-playbook.yaml", yamlContent);

        final Playbook playbook = parser.parse("test-playbook.yaml", manager);
        assertNotNull(playbook);

        final Map<String, String> addons = playbook.getPromptAddons();
        assertNotNull(addons);
        assertEquals("Default general prompt addon", addons.get("default"));
        assertEquals("Specific pesap addon", addons.get("pesap"));
        assertEquals("Specific general addon", addons.get("general"));
    }

    /**
     * Verifies that nested canonical promptAddon map is parsed correctly.
     *
     * @throws IOException if parsing fails
     */
    @Test
    public void testYamlParsingCanonicalPromptAddonMap() throws IOException
    {
        final String yamlContent = """
            promptAddon:
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

        final Map<String, String> addons = playbook.getPromptAddons();
        assertNotNull(addons);
        assertEquals("Nested pesap rule", addons.get("pesap"));
        assertEquals("Nested general rule", addons.get("general"));
    }

    /**
     * Verifies that prompt add-ons accumulate across model, playbook, and dataset layers.
     */
    @Test
    public void testMultiLayerAccumulation()
    {
        // 1. Setup ExecutionContext with SessionData (Dataset layer)
        final Map<String, SessionData.DataEntry> staticData = new HashMap<>();
        staticData.put("promptAddon", new SessionData.DataEntry("Dataset general rule", false));
        staticData.put("promptAddon.pesap", new SessionData.DataEntry("Dataset pesap rule", false));
        final SessionData sessionData = new SessionData(staticData);
        final ExecutionContext context = new ExecutionContext(sessionData);

        // 2. Setup Playbook layer in transient data
        final Map<String, String> yamlAddons = new HashMap<>();
        yamlAddons.put("default", "Playbook general rule");
        yamlAddons.put("pesap", "Playbook pesap rule");
        context.getTransientData().put("playbook.promptAddons", yamlAddons);

        // For pesap query: Playbook general + Playbook pesap + Dataset general + Dataset pesap
        final String pesapAddon = SystemPromptAddonHelper.getAddon("pesap", context);
        assertNotNull(pesapAddon);
        final String expectedPesap = "Playbook general rule\n\nPlaybook pesap rule\n\nDataset general rule\n\nDataset pesap rule";
        assertEquals(expectedPesap, pesapAddon);

        // For general query: Playbook general + Dataset general
        final String generalAddon = SystemPromptAddonHelper.getAddon("general", context);
        assertNotNull(generalAddon);
        final String expectedGeneral = "Playbook general rule\n\nDataset general rule";
        assertEquals(expectedGeneral, generalAddon);
    }

    /**
     * Verifies that ${variable} placeholders in prompt add-ons are dynamically interpolated.
     */
    @Test
    public void testVariableInterpolationInPromptAddon()
    {
        // Setup Dataset variables
        final Map<String, SessionData.DataEntry> staticData = new HashMap<>();
        staticData.put("targetLocale", new SessionData.DataEntry("French (Canada)", false));
        staticData.put("userName", new SessionData.DataEntry("Jean Dupont", false));
        staticData.put("promptAddon", new SessionData.DataEntry("Dataset user: ${userName}", false));
        final SessionData sessionData = new SessionData(staticData);
        final ExecutionContext context = new ExecutionContext(sessionData);

        // Setup Playbook add-on with variable reference
        final Map<String, String> yamlAddons = new HashMap<>();
        yamlAddons.put("default", "Translate text to ${targetLocale}");
        context.getTransientData().put("playbook.promptAddons", yamlAddons);

        final String result = SystemPromptAddonHelper.getAddon("general", context);
        assertNotNull(result);
        assertEquals("Translate text to French (Canada)\n\nDataset user: Jean Dupont", result);
    }

    /**
     * Verifies that unresolvable ${variable} placeholder throws IllegalArgumentException.
     */
    @Test
    public void testUnresolvableVariableThrowsException()
    {
        final SessionData emptyData = new SessionData(Collections.emptyMap());
        final ExecutionContext context = new ExecutionContext(emptyData);

        final Map<String, String> yamlAddons = new HashMap<>();
        yamlAddons.put("default", "Hello ${nonExistentVariable}");
        context.getTransientData().put("playbook.promptAddons", yamlAddons);

        assertThrows(IllegalArgumentException.class, () -> {
            SystemPromptAddonHelper.getAddon("general", context);
        });
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
        context.getTransientData().put("playbook.promptAddons", yamlAddons);

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
        context.getTransientData().put("playbook.promptAddons", yamlAddons);

        assertThrows(IllegalArgumentException.class, () -> {
            SystemPromptAddonHelper.getAddon("general", context);
        });
    }

    /**
     * Verifies that when multilingual guidance is enabled, Language Universality is appended.
     */
    @Test
    public void testMultilingualAddonInjection()
    {
        final Map<String, SessionData.DataEntry> data = new HashMap<>();
        data.put("neodymium.ai.multilingual", new SessionData.DataEntry("true", false));
        final ExecutionContext context = new ExecutionContext(new SessionData(data));

        final String pesapAddon = SystemPromptAddonHelper.getAddon("pesap", context);
        assertNotNull(pesapAddon);
        assertTrue(pesapAddon.contains("Language Universality"));
        assertTrue(pesapAddon.contains("sub-steps"));

        final String generalAddon = SystemPromptAddonHelper.getAddon("general", context);
        assertNotNull(generalAddon);
        assertTrue(generalAddon.contains("Language Universality"));
        assertTrue(generalAddon.contains("button texts, labels"));

        final String basePrompt = "Base system prompt instructions";
        final String combined = SystemPromptAddonHelper.appendAddon(basePrompt, "pesap", context);
        assertNotNull(combined);
        assertTrue(combined.contains("Language Universality"));
        assertTrue(combined.contains("CRITICAL REMINDER"));
    }
}
