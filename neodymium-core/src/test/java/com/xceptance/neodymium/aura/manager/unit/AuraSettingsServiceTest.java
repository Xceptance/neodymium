package com.xceptance.neodymium.aura.manager.unit;

import com.xceptance.neodymium.aura.AuraSettingsService;
import com.xceptance.neodymium.aura.AuraSettingsService.PropertyGroupDto;
import com.xceptance.neodymium.aura.AuraSettingsService.SettingsDataDto;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
        Assertions.assertTrue(updatedContent.contains("neodymium.ai.generate = true"));
        Assertions.assertTrue(updatedContent.contains("neodymium.ai.apiKey = newKey"));
        Assertions.assertTrue(updatedContent.contains("# AI Test Prompt Generation"));
    }
}
