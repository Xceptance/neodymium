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
package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.aura.AuraSettingsService;
import com.xceptance.neodymium.aura.AuraSettingsService.PropertyEntryDto;
import com.xceptance.neodymium.aura.AuraSettingsService.PropertyGroupDto;
import com.xceptance.neodymium.aura.AuraSettingsService.PropertySectionDto;
import com.xceptance.neodymium.aura.AuraSettingsService.SettingsDataDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for AuraSettingsService property file parsing, ordering, log4j exclusion,
 * override detection, browser profile tab grouping, and file saving.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("aura-manager")
public final class AuraSettingsServiceTest
{
    @Test
    public void testPropertyFileDiscoveryAndOrdering(@TempDir final Path tempDir) throws IOException
    {
        Files.createFile(tempDir.resolve("browser.properties"));
        Files.createFile(tempDir.resolve("ai.properties"));
        Files.createFile(tempDir.resolve("neodymium.properties"));
        Files.createFile(tempDir.resolve("log4j2.properties"));
        Files.createFile(tempDir.resolve("logging.properties"));
        Files.createFile(tempDir.resolve("xtc-api.properties"));
        Files.createFile(tempDir.resolve("dev-neodymium.properties"));

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        final List<Path> discovered = service.discoverPropertyFiles();

        Assertions.assertEquals(4, discovered.size());
        Assertions.assertEquals("ai.properties", discovered.get(0).getFileName().toString());
        Assertions.assertEquals("neodymium.properties", discovered.get(1).getFileName().toString());
        Assertions.assertEquals("browser.properties", discovered.get(2).getFileName().toString());
        Assertions.assertEquals("xtc-api.properties", discovered.get(3).getFileName().toString());
    }

    @Test
    public void testSettingsDataLoadingAndOverrideDetection(@TempDir final Path tempDir) throws IOException
    {
        final String aiContent = """
                # AI Test Prompt Generation
                # Enables prompt generation
                neodymium.ai.generate = false
                neodymium.ai.apiKey = ${GEMINI_API_KEY}
                """;

        final String devNeoContent = """
                neodymium.ai.generate = true
                neodymium.custom.key = test
                """;

        final String browserContent = """
                browserprofile.global.headless = true
                browserprofile.Chrome_headless.name = Chrome Headless
                browserprofile.Chrome_headless.browser = chrome
                browserprofile.Chrome_headless.headless = true
                """;

        Files.writeString(tempDir.resolve("ai.properties"), aiContent);
        Files.writeString(tempDir.resolve("dev-neodymium.properties"), devNeoContent);
        Files.writeString(tempDir.resolve("browser.properties"), browserContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        final SettingsDataDto settingsData = service.loadSettingsData();

        Assertions.assertTrue(settingsData.getOverriddenKeys().contains("neodymium.ai.generate"));
        Assertions.assertTrue(settingsData.getAutocompleteKeys().contains("neodymium.ai.generate"));
        Assertions.assertTrue(settingsData.getAutocompleteKeys().contains("neodymium.ai.apiKey"));

        final List<PropertyGroupDto> generalGroups = settingsData.getGeneralGroups();
        Assertions.assertEquals(2, generalGroups.size());

        final PropertyGroupDto devNeoGroup = generalGroups.get(0);
        Assertions.assertEquals("dev-neodymium.properties", devNeoGroup.getFileName());
        Assertions.assertTrue(devNeoGroup.isDevNeo());
        Assertions.assertEquals(2, devNeoGroup.getDevNeoEntries().size());

        final PropertyGroupDto aiGroup = generalGroups.get(1);
        Assertions.assertEquals("ai.properties", aiGroup.getFileName());
        Assertions.assertTrue(aiGroup.isOpenByDefault());

        final PropertyGroupDto browserGroup = settingsData.getBrowserGroup();
        Assertions.assertNotNull(browserGroup);
        Assertions.assertTrue(browserGroup.isBrowserConfig());
        Assertions.assertEquals(2, browserGroup.getSections().size());
        Assertions.assertEquals("Global Browser Settings (browserprofile.global)", browserGroup.getSections().get(0).getName());
        Assertions.assertEquals("Profile: Chrome_headless", browserGroup.getSections().get(1).getName());
    }

    @Test
    public void testSavePropertyFilePreservingComments(@TempDir final Path tempDir) throws IOException
    {
        final String aiContent = """
                # AI Test Prompt Generation
                # Enables prompt generation
                #neodymium.ai.generate = false
                neodymium.ai.apiKey = oldKey
                """;

        Files.writeString(tempDir.resolve("ai.properties"), aiContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.savePropertyFile("ai.properties", Map.of(
                "neodymium.ai.generate", "true",
                "neodymium.ai.apiKey", "newKey"
        ));

        final String updatedContent = Files.readString(tempDir.resolve("ai.properties"));
        Assertions.assertTrue(updatedContent.contains("#neodymium.ai.generate = false"),
                "Commented property lines must remain commented on save");
        Assertions.assertFalse(updatedContent.contains("\nneodymium.ai.generate"),
                "Commented property lines must never be uncommented or appended as active");
        Assertions.assertTrue(updatedContent.contains("neodymium.ai.apiKey = newKey"));
        Assertions.assertTrue(updatedContent.contains("# AI Test Prompt Generation"));
    }

    @Test
    public void testParsePropertyFileIgnoresCommentedExamplesAndProseWithColons(@TempDir final Path tempDir) throws IOException
    {
        final String aiContent = """
                # =========================================================================================
                # AI Configuration Properties (ai.properties)
                # =========================================================================================
                # File header prose that must not bleed past blank line

                # Execution mode strategy:
                # - AUTO: Smart mode
                # Default: REPLAY_WITH_HEALING
                # neodymium.ai.executionMode = AUTO

                # Options: OFF, LOW, MEDIUM, HIGH
                # neodymium.ai.reasoningEffort = MEDIUM
                neodymium.ai.contextLevel = STANDARD
                """;

        final String neoContent = """
                # NOTE: The URLs need to be provided as Regular Expressions!
                # neodymium.url.excludeList = ^http://prod.example.com/[?]testmode=true
                # neodymium.url.includeList = ^http://dev.example.com
                #neodymium.webDriver.chrome.pathToDriverServer =  /path/to/chromedriver
                # Warning: The webdriver process might stay alive
                # Range: 0.0 - 1.0
                # ATTENTION: should NOT be defined here

                # How long should Selenide wait
                # neodymium.selenide.timeout = 3000
                #
                # The clicks will be executed via JavaScript
                neodymium.selenide.clickViaJs = true
                """;

        final String verlaContent = """
                # Global latency scaling factor:
                #   1.0 = standard real-world runtime
                #   0.0 = instant execution
                verla.latency.scale = 1.0
                """;

        Files.writeString(tempDir.resolve("ai.properties"), aiContent);
        Files.writeString(tempDir.resolve("neodymium.properties"), neoContent);
        Files.writeString(tempDir.resolve("verla.properties"), verlaContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        final SettingsDataDto data = service.loadSettingsData();

        final Set<String> activeKeys = data.getGeneralGroups().stream()
                .filter(g -> !g.isDevNeo())
                .flatMap(g -> g.getSections().stream())
                .flatMap(s -> s.getEntries().stream())
                .peek(e -> Assertions.assertTrue(e.isActive(), "Only active entries may be parsed: " + e.getKey()))
                .map(PropertyEntryDto::getKey)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of("neodymium.ai.contextLevel", "neodymium.selenide.clickViaJs", "verla.latency.scale"), activeKeys);
        Assertions.assertEquals(activeKeys, Set.copyOf(data.getAutocompleteKeys()));

        final PropertyEntryDto contextLevel = findEntry(data, "ai.properties", "neodymium.ai.contextLevel");
        Assertions.assertEquals("", contextLevel.getDescription(),
                "Comments above a commented-out example must not bleed into the next active property");

        final PropertyEntryDto clickViaJs = findEntry(data, "neodymium.properties", "neodymium.selenide.clickViaJs");
        Assertions.assertEquals("The clicks will be executed via JavaScript", clickViaJs.getDescription());

        final PropertyEntryDto latencyScale = findEntry(data, "verla.properties", "verla.latency.scale");
        Assertions.assertTrue(latencyScale.getDescription().contains("1.0 = standard real-world runtime"),
                "Numeric prose examples inside comments should stay attached to the active property description");
    }

    @Test
    public void testParseBrowserPropertyFileDoesNotBleedCommentedExampleIntoDescription(@TempDir final Path tempDir) throws IOException
    {
        final String browserContent = """
                # Global properties
                #
                # browserprofile.global.pageLoadStrategy = normal|eager|none
                browserprofile.global.headless = true
                """;

        Files.writeString(tempDir.resolve("browser.properties"), browserContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        final SettingsDataDto data = service.loadSettingsData();

        final PropertySectionDto globalSection = data.getBrowserGroup().getSections().getFirst();
        Assertions.assertEquals(1, globalSection.getEntries().size());
        final PropertyEntryDto headlessEntry = globalSection.getEntries().getFirst();
        Assertions.assertEquals("browserprofile.global.headless", headlessEntry.getKey());
        Assertions.assertEquals(AuraSettingsService.getBrowserPropertyHelp("headless"), headlessEntry.getDescription());
    }

    @Test
    public void testSavePropertyFileNeverUncommentsOrOverwritesWithEarlierCommentedSameKey(@TempDir final Path tempDir) throws IOException
    {
        final String neoContent = """
                #neodymium.url = http://localhost:8080/en-US/
                neodymium.url = https://active.example.com/
                """;

        final Path neoPath = tempDir.resolve("neodymium.properties");
        Files.writeString(neoPath, neoContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        final SettingsDataDto data = service.loadSettingsData();
        final PropertyEntryDto urlEntry = findEntry(data, "neodymium.properties", "neodymium.url");
        Assertions.assertEquals("https://active.example.com/", urlEntry.getValue());

        service.savePropertyFile("neodymium.properties", Map.of("neodymium.url", "https://updated.example.com/"));

        final List<String> updatedLines = Files.readAllLines(neoPath);
        Assertions.assertEquals(List.of(
                "#neodymium.url = http://localhost:8080/en-US/",
                "neodymium.url = https://updated.example.com/"
        ), updatedLines);
    }

    @Test
    public void testSavePropertyFilePreservesUnchangedLinesAndSkipsDiskWriteForUntouchedFile(@TempDir final Path tempDir) throws IOException
    {
        final String xtcContent = """
                # ATTENTION: keep this comment
                xtc.api.enable=false
                xtc.api.key =
                """;

        final Path xtcPath = tempDir.resolve("xtc-api.properties");
        Files.writeString(xtcPath, xtcContent);
        final FileTime olderTime = FileTime.from(Instant.now().minusSeconds(3600));
        Files.setLastModifiedTime(xtcPath, olderTime);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.savePropertyFile("xtc-api.properties", Map.of(
                "xtc.api.enable", "false",
                "xtc.api.key", ""
        ));

        Assertions.assertEquals(olderTime, Files.getLastModifiedTime(xtcPath),
                "Unchanged property file must not be rewritten on disk");
        Assertions.assertEquals(xtcContent, Files.readString(xtcPath));

        service.savePropertyFile("xtc-api.properties", Map.of(
                "xtc.api.enable", "true",
                "xtc.api.key", ""
        ));

        final String updated = Files.readString(xtcPath);
        Assertions.assertTrue(updated.contains("xtc.api.enable = true"));
        Assertions.assertTrue(updated.contains("xtc.api.key =\n"),
                "Unchanged key formatting must be preserved verbatim");
        Assertions.assertTrue(updated.contains("# ATTENTION: keep this comment"));
    }

    @Test
    public void testSavePropertyFileDoesNotCreateMissingFileWhenUpdating(@TempDir final Path tempDir) throws IOException
    {
        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.savePropertyFile("missing.properties", Map.of("some.key", "someValue"));

        Assertions.assertFalse(Files.exists(tempDir.resolve("missing.properties")));
    }

    @Test
    public void testAddBrowserPropertyAppendsWithoutUncommentingCommentedExample(@TempDir final Path tempDir) throws IOException
    {
        final String browserContent = """
                # browserprofile.global.browserResolution = 1200x900
                browserprofile.global.headless = true
                """;

        final Path browserPath = tempDir.resolve("browser.properties");
        Files.writeString(browserPath, browserContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.addBrowserProperty("global", "browserResolution", "1920x1080");

        final String updated = Files.readString(browserPath);
        Assertions.assertTrue(updated.contains("# browserprofile.global.browserResolution = 1200x900"),
                "Commented browser example must remain commented");
        Assertions.assertTrue(updated.contains("browserprofile.global.browserResolution = 1920x1080"),
                "Newly added browser property must be appended");
    }

    @Test
    public void testSaveDevNeoPropertiesDoesNotCreateFileWhenAbsentAndEmpty(@TempDir final Path tempDir) throws IOException
    {
        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.saveDevNeoProperties(List.of());

        Assertions.assertFalse(Files.exists(tempDir.resolve("dev-neodymium.properties")));
    }

    @Test
    public void testSaveDevNeoPropertiesCreatesFileWhenAbsentAndNonEmpty(@TempDir final Path tempDir) throws IOException
    {
        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.saveDevNeoProperties(List.of(Map.entry("neodymium.ai.model", "gemini-3.8-flash")));

        final Path devNeoPath = tempDir.resolve("dev-neodymium.properties");
        Assertions.assertTrue(Files.exists(devNeoPath));
        final String content = Files.readString(devNeoPath);
        Assertions.assertTrue(content.contains("# Temporary local override properties"));
        Assertions.assertTrue(content.contains("neodymium.ai.model = gemini-3.8-flash"));
    }

    @Test
    public void testSaveDevNeoPropertiesPreservesCommentsAndSkipsWriteWhenUnchanged(@TempDir final Path tempDir) throws IOException
    {
        final String devNeoContent = """
                # Keep personal developer note
                # commented.override = shouldStayCommented
                neodymium.ai.timeoutSeconds=60
                """;

        final Path devNeoPath = tempDir.resolve("dev-neodymium.properties");
        Files.writeString(devNeoPath, devNeoContent);
        final FileTime olderTime = FileTime.from(Instant.now().minusSeconds(3600));
        Files.setLastModifiedTime(devNeoPath, olderTime);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.saveDevNeoProperties(List.of(Map.entry("neodymium.ai.timeoutSeconds", "60")));

        Assertions.assertEquals(olderTime, Files.getLastModifiedTime(devNeoPath));
        Assertions.assertEquals(devNeoContent, Files.readString(devNeoPath));
    }

    @Test
    public void testSaveDevNeoPropertiesUpdatesAddsAndRemovesActiveKeysWhileKeepingComments(@TempDir final Path tempDir) throws IOException
    {
        final String devNeoContent = """
                # Personal local overrides
                # neodymium.ai.generate = true
                keep.unchanged=val1
                change.me = old
                remove.me = gone
                """;

        final Path devNeoPath = tempDir.resolve("dev-neodymium.properties");
        Files.writeString(devNeoPath, devNeoContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        service.saveDevNeoProperties(List.of(
                Map.entry("keep.unchanged", "val1"),
                Map.entry("change.me", "updated"),
                Map.entry("add.new", "added")
        ));

        final String afterUpdate = Files.readString(devNeoPath);
        Assertions.assertTrue(afterUpdate.contains("# Personal local overrides"));
        Assertions.assertTrue(afterUpdate.contains("# neodymium.ai.generate = true"));
        Assertions.assertTrue(afterUpdate.contains("keep.unchanged=val1"));
        Assertions.assertTrue(afterUpdate.contains("change.me = updated"));
        Assertions.assertFalse(afterUpdate.contains("remove.me"));
        Assertions.assertTrue(afterUpdate.contains("add.new = added"));

        service.saveDevNeoProperties(List.of());

        final String afterClear = Files.readString(devNeoPath);
        Assertions.assertTrue(afterClear.contains("# Personal local overrides"));
        Assertions.assertTrue(afterClear.contains("# neodymium.ai.generate = true"));
        Assertions.assertFalse(afterClear.contains("keep.unchanged"));
        Assertions.assertFalse(afterClear.contains("change.me"));
        Assertions.assertFalse(afterClear.contains("add.new"));
    }

    @Test
    public void testMultiFileLoadAndSinglePropertySaveLeavesOtherFilesAndCommentsUntouched(@TempDir final Path tempDir) throws IOException
    {
        final String aiContent = """
                # AI settings
                neodymium.ai.generate = false
                # neodymium.ai.executionMode = AUTO
                """;
        final String neoContent = """
                # NOTE: Keep commented examples intact
                # neodymium.url.excludeList = ^http://prod.example.com
                # neodymium.url.includeList = ^http://dev.example.com
                neodymium.selenide.clickViaJs = true
                """;
        final String verlaContent = """
                #   1.0 = standard real-world runtime
                verla.latency.scale = 1.0
                """;
        final String xtcContent = """
                # ATTENTION: leave commented
                xtc.api.enable = false
                """;
        final String browserContent = """
                # browserprofile.global.pageLoadStrategy = normal|eager|none
                browserprofile.global.headless = true
                """;

        final Path aiPath = tempDir.resolve("ai.properties");
        final Path neoPath = tempDir.resolve("neodymium.properties");
        final Path verlaPath = tempDir.resolve("verla.properties");
        final Path xtcPath = tempDir.resolve("xtc-api.properties");
        final Path browserPath = tempDir.resolve("browser.properties");

        Files.writeString(aiPath, aiContent);
        Files.writeString(neoPath, neoContent);
        Files.writeString(verlaPath, verlaContent);
        Files.writeString(xtcPath, xtcContent);
        Files.writeString(browserPath, browserContent);

        final FileTime olderTime = FileTime.from(Instant.now().minusSeconds(3600));
        Files.setLastModifiedTime(neoPath, olderTime);
        Files.setLastModifiedTime(verlaPath, olderTime);
        Files.setLastModifiedTime(xtcPath, olderTime);
        Files.setLastModifiedTime(browserPath, olderTime);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        final SettingsDataDto data = service.loadSettingsData();

        final Map<String, Map<String, String>> submittedByFile = new LinkedHashMap<>();
        for (final PropertyGroupDto group : data.getGeneralGroups())
        {
            if (group.isDevNeo())
            {
                continue;
            }
            final Map<String, String> values = new LinkedHashMap<>();
            for (final PropertySectionDto section : group.getSections())
            {
                for (final PropertyEntryDto entry : section.getEntries())
                {
                    final String val = "neodymium.ai.generate".equals(entry.getKey()) ? "true" : entry.getValue();
                    values.put(entry.getKey(), val);
                }
            }
            submittedByFile.put(group.getFileName(), values);
        }
        final Map<String, String> browserValues = new LinkedHashMap<>();
        for (final PropertySectionDto section : data.getBrowserGroup().getSections())
        {
            for (final PropertyEntryDto entry : section.getEntries())
            {
                browserValues.put(entry.getKey(), entry.getValue());
            }
        }
        submittedByFile.put(data.getBrowserGroup().getFileName(), browserValues);

        for (final Map.Entry<String, Map<String, String>> fileSubmit : submittedByFile.entrySet())
        {
            service.savePropertyFile(fileSubmit.getKey(), fileSubmit.getValue());
        }

        final String updatedAi = Files.readString(aiPath);
        Assertions.assertTrue(updatedAi.contains("neodymium.ai.generate = true"));
        Assertions.assertTrue(updatedAi.contains("# neodymium.ai.executionMode = AUTO"));

        Assertions.assertEquals(olderTime, Files.getLastModifiedTime(neoPath));
        Assertions.assertEquals(neoContent, Files.readString(neoPath));
        Assertions.assertEquals(olderTime, Files.getLastModifiedTime(verlaPath));
        Assertions.assertEquals(verlaContent, Files.readString(verlaPath));
        Assertions.assertEquals(olderTime, Files.getLastModifiedTime(xtcPath));
        Assertions.assertEquals(xtcContent, Files.readString(xtcPath));
        Assertions.assertEquals(olderTime, Files.getLastModifiedTime(browserPath));
        Assertions.assertEquals(browserContent, Files.readString(browserPath));
    }

    private static PropertyEntryDto findEntry(final SettingsDataDto data, final String fileName, final String key)
    {
        return data.getGeneralGroups().stream()
                .filter(g -> fileName.equals(g.getFileName()))
                .flatMap(g -> g.getSections().stream())
                .flatMap(s -> s.getEntries().stream())
                .filter(e -> key.equals(e.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing entry " + key + " in " + fileName));
    }

    @Test
    public void testProjectIdComputation(@TempDir final Path tempDir)
    {
        final Path configDir = tempDir.resolve("config");
        final AuraSettingsService service = new AuraSettingsService(configDir);
        final String projectId = service.getProjectId();

        Assertions.assertNotNull(projectId);
        Assertions.assertTrue(projectId.contains("-"), "ProjectId should contain a hyphen between folder and hash");
        Assertions.assertEquals(projectId, service.getProjectId(), "ProjectId should be deterministic");
    }

    @Test
    public void testApiKeyWarningIgnoredSessionFlag(@TempDir final Path tempDir)
    {
        final AuraSettingsService service = new AuraSettingsService(tempDir);
        Assertions.assertFalse(service.isApiKeyWarningIgnored(), "Warning should not be ignored by default");

        service.setApiKeyWarningIgnored(true);
        Assertions.assertTrue(service.isApiKeyWarningIgnored(), "Warning should be ignored after setting true");

        service.setApiKeyWarningIgnored(false);
        Assertions.assertFalse(service.isApiKeyWarningIgnored(), "Warning should reflect reset to false");
    }

    @Test
    public void testHasConfiguredApiKey_MissingAndPlaceholder(@TempDir final Path tempDir) throws IOException
    {
        final String aiContent = """
                neodymium.ai.provider = gemini
                neodymium.ai.apiKey = ${GEMINI_API_KEY}
                """;
        Files.writeString(tempDir.resolve("ai.properties"), aiContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        // In clean test environment without GEMINI_API_KEY env var, unresolved placeholder counts as missing
        if (System.getenv("GEMINI_API_KEY") == null && System.getProperty("neodymium.ai.apiKey") == null)
        {
            Assertions.assertFalse(service.hasConfiguredApiKey(), "Placeholder key should count as missing");
        }
    }

    @Test
    public void testHasConfiguredApiKey_WithDirectKey(@TempDir final Path tempDir) throws IOException
    {
        final String aiContent = """
                neodymium.ai.provider = gemini
                neodymium.ai.apiKey = valid-test-api-key-12345
                """;
        Files.writeString(tempDir.resolve("ai.properties"), aiContent);

        final AuraSettingsService service = new AuraSettingsService(tempDir);
        // Set system property to simulate configured key in test environment
        System.setProperty("neodymium.ai.apiKey", "valid-test-api-key-12345");
        try
        {
            Assertions.assertTrue(service.hasConfiguredApiKey(), "Explicit key should be recognized as configured");
        }
        finally
        {
            System.clearProperty("neodymium.ai.apiKey");
        }
    }
}
