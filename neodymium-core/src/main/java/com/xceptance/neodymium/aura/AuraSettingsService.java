/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.aura;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for loading, parsing, ordering, and saving Neodymium property files
 * for the Aura Manager Settings dialog. Handles standard properties and special browser properties.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraSettingsService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraSettingsService.class);
    private static final String DEV_NEO_FILE = "dev-neodymium.properties";
    private static final String BROWSER_FILE = "browser.properties";
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*([a-zA-Z0-9_.-]+)\\s*[:=]\\s*(.*)$");
    private static final Pattern COMMENTED_KEY_PATTERN = Pattern.compile("^\\s*[#!]\\s*([a-zA-Z0-9_.-]+)\\s*[:=]\\s*(.*)$");

    private final Path configDirectory;

    public AuraSettingsService()
    {
        this(Path.of("config"));
    }

    public AuraSettingsService(final Path configDirectory)
    {
        this.configDirectory = configDirectory;
    }

    /**
     * DTO representing an individual property entry.
     */
    public static final class PropertyEntryDto
    {
        private final String key;
        private final String value;
        private final String description;
        private final boolean active;
        private final boolean overridden;
        private final String inputType;

        public PropertyEntryDto(final String key, final String value, final String description,
                final boolean active, final boolean overridden, final String inputType)
        {
            this.key = key;
            this.value = value;
            this.description = description;
            this.active = active;
            this.overridden = overridden;
            this.inputType = inputType;
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }

        public String getDescription()
        {
            return description;
        }

        public boolean isActive()
        {
            return active;
        }

        public boolean isOverridden()
        {
            return overridden;
        }

        public String getInputType()
        {
            return inputType;
        }
    }

    /**
     * DTO representing a section grouping within a property file or browser profile.
     */
    public static final class PropertySectionDto
    {
        private final String name;
        private final String profileTag;
        private final String browserIcon;
        private final List<PropertyEntryDto> entries;
        private final List<String> missingExpectedProperties;

        public PropertySectionDto(final String name, final List<PropertyEntryDto> entries)
        {
            this(name, "", "description", entries, List.of());
        }

        public PropertySectionDto(final String name, final String profileTag, final String browserIcon,
                final List<PropertyEntryDto> entries, final List<String> missingExpectedProperties)
        {
            this.name = name;
            this.profileTag = profileTag;
            this.browserIcon = browserIcon;
            this.entries = entries;
            this.missingExpectedProperties = missingExpectedProperties;
        }

        public String getName()
        {
            return name;
        }

        public String getProfileTag()
        {
            return profileTag;
        }

        public String getBrowserIcon()
        {
            return browserIcon;
        }

        public List<PropertyEntryDto> getEntries()
        {
            return entries;
        }

        public List<String> getMissingExpectedProperties()
        {
            return missingExpectedProperties;
        }
    }

    /**
     * List of standard expected browser profile properties in Neodymium.
     */
    public static final List<String> EXPECTED_BROWSER_PROPERTIES = List.of(
            "name",
            "browser",
            "headless",
            "browserResolution",
            "arguments",
            "driverArgs",
            "preferences",
            "version",
            "pageLoadStrategy",
            "acceptInsecureCertificates",
            "screenResolution",
            "platform",
            "deviceOrientation",
            "testEnvironment",
            "chromeEmulationProfile",
            "downloadDirectory"
    );

    /**
     * List of standard expected global browser properties in Neodymium (browserprofile.global.*).
     */
    public static final List<String> EXPECTED_GLOBAL_PROPERTIES = List.of(
            "headless",
            "browserResolution",
            "pageLoadStrategy",
            "acceptInsecureCertificates",
            "downloadDirectory",
            "arguments",
            "preferences",
            "screenResolution",
            "testEnvironment"
    );

    public static String getBrowserIcon(final String browserType, final String profileTag)
    {
        if ("global".equalsIgnoreCase(profileTag))
        {
            return "tune";
        }
        if (browserType == null || browserType.trim().isEmpty())
        {
            return "desktop_windows";
        }
        final String lower = browserType.trim().toLowerCase();
        if (lower.contains("chrome") || lower.contains("firefox") || lower.contains("ff")
                || lower.contains("safari") || lower.contains("edge") || lower.contains("ie"))
        {
            return "language";
        }
        if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("android") || lower.contains("mobile"))
        {
            return "smartphone";
        }
        return "desktop_windows";
    }


    /**
     * DTO representing a property file group.
     */
    public static final class PropertyGroupDto
    {
        private final String fileName;
        private final String displayName;
        private final boolean openByDefault;
        private final boolean devNeo;
        private final boolean browserConfig;
        private final List<PropertySectionDto> sections;
        private final List<Map.Entry<String, String>> devNeoEntries;

        public PropertyGroupDto(final String fileName, final String displayName, final boolean openByDefault,
                final boolean devNeo, final boolean browserConfig, final List<PropertySectionDto> sections,
                final List<Map.Entry<String, String>> devNeoEntries)
        {
            this.fileName = fileName;
            this.displayName = displayName;
            this.openByDefault = openByDefault;
            this.devNeo = devNeo;
            this.browserConfig = browserConfig;
            this.sections = sections;
            this.devNeoEntries = devNeoEntries;
        }

        public String getFileName()
        {
            return fileName;
        }

        public String getDisplayName()
        {
            return displayName;
        }

        public boolean isOpenByDefault()
        {
            return openByDefault;
        }

        public boolean isDevNeo()
        {
            return devNeo;
        }

        public boolean isBrowserConfig()
        {
            return browserConfig;
        }

        public List<PropertySectionDto> getSections()
        {
            return sections;
        }

        public List<Map.Entry<String, String>> getDevNeoEntries()
        {
            return devNeoEntries;
        }
    }

    /**
     * DTO containing separated settings groups, autocomplete suggestions, and override info.
     */
    public static final class SettingsDataDto
    {
        private final List<PropertyGroupDto> generalGroups;
        private final PropertyGroupDto browserGroup;
        private final List<String> autocompleteKeys;
        private final Set<String> overriddenKeys;

        public SettingsDataDto(final List<PropertyGroupDto> generalGroups, final PropertyGroupDto browserGroup,
                final List<String> autocompleteKeys, final Set<String> overriddenKeys)
        {
            this.generalGroups = generalGroups;
            this.browserGroup = browserGroup;
            this.autocompleteKeys = autocompleteKeys;
            this.overriddenKeys = overriddenKeys;
        }

        public List<PropertyGroupDto> getGeneralGroups()
        {
            return generalGroups;
        }

        public PropertyGroupDto getBrowserGroup()
        {
            return browserGroup;
        }

        public List<String> getAutocompleteKeys()
        {
            return autocompleteKeys;
        }

        public Set<String> getOverriddenKeys()
        {
            return overriddenKeys;
        }
    }

    /**
     * Reads all property files from config directory and builds structured settings data.
     */
    public SettingsDataDto loadSettingsData()
    {
        final Map<String, String> devNeoMap = loadDevNeoProperties();
        final Set<String> overriddenKeys = new HashSet<>(devNeoMap.keySet());
        final Set<String> allKnownKeys = new TreeSet<>();

        final List<Path> propertyFiles = discoverPropertyFiles();
        final List<PropertyGroupDto> generalGroups = new ArrayList<>();
        PropertyGroupDto browserGroup = null;

        for (final Path path : propertyFiles)
        {
            final String fileName = path.getFileName().toString();
            if (DEV_NEO_FILE.equalsIgnoreCase(fileName))
            {
                continue;
            }

            if (BROWSER_FILE.equalsIgnoreCase(fileName))
            {
                final List<PropertySectionDto> browserSections = parseBrowserPropertyFile(path, overriddenKeys, allKnownKeys);
                browserGroup = new PropertyGroupDto(fileName, "Browser Profiles & Settings", true, false, true, browserSections, List.of());
                continue;
            }

            final boolean isOpenByDefault = "ai.properties".equalsIgnoreCase(fileName);
            final List<PropertySectionDto> sections = parsePropertyFile(path, overriddenKeys, allKnownKeys);

            generalGroups.add(new PropertyGroupDto(fileName, fileName, isOpenByDefault, false, false, sections, List.of()));
        }

        // Add dev-neodymium.properties group at top (index 0) of general groups
        final List<Map.Entry<String, String>> devNeoEntries = new ArrayList<>();
        for (final Map.Entry<String, String> entry : devNeoMap.entrySet())
        {
            devNeoEntries.add(Map.entry(entry.getKey(), entry.getValue()));
        }

        generalGroups.add(0, new PropertyGroupDto(DEV_NEO_FILE, DEV_NEO_FILE, false, true, false, List.of(), devNeoEntries));

        if (browserGroup == null)
        {
            browserGroup = new PropertyGroupDto(BROWSER_FILE, "Browser Profiles & Settings", true, false, true, List.of(), List.of());
        }

        // Filter autocomplete keys so dev-neodymium autocomplete does NOT suggest browserprofile.* keys
        final List<String> filteredAutocompleteKeys = allKnownKeys.stream()
                .filter(k -> !k.toLowerCase().startsWith("browserprofile."))
                .toList();

        return new SettingsDataDto(generalGroups, browserGroup, filteredAutocompleteKeys, overriddenKeys);
    }

    /**
     * Discovers `.properties` files in `config/` excluding log4j/logging files and orders them:
     * 1. ai.properties
     * 2. neodymium.properties
     * 3. everything else alphabetically
     */
    public List<Path> discoverPropertyFiles()
    {
        final List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(configDirectory))
        {
            return result;
        }

        try (final var stream = Files.list(configDirectory))
        {
            final List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        final String name = path.getFileName().toString().toLowerCase();
                        if (!name.endsWith(".properties"))
                        {
                            return false;
                        }
                        if (name.contains("log4j") || name.startsWith("logging") || name.endsWith(".temp"))
                        {
                            return false;
                        }
                        if (name.startsWith("temp-") || name.equalsIgnoreCase(DEV_NEO_FILE))
                        {
                            return false;
                        }
                        return true;
                    })
                    .toList();

            final Comparator<Path> pathComparator = (p1, p2) -> {
                final String n1 = p1.getFileName().toString().toLowerCase();
                final String n2 = p2.getFileName().toString().toLowerCase();

                if (n1.equals("ai.properties"))
                {
                    return -1;
                }
                if (n2.equals("ai.properties"))
                {
                    return 1;
                }
                if (n1.equals("neodymium.properties"))
                {
                    return -1;
                }
                if (n2.equals("neodymium.properties"))
                {
                    return 1;
                }
                return n1.compareTo(n2);
            };

            final List<Path> sorted = new ArrayList<>(files);
            sorted.sort(pathComparator);
            result.addAll(sorted);
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to list property files in {}", configDirectory, e);
        }

        return result;
    }

    /**
     * Reads `dev-neodymium.properties` if present, preserving order.
     */
    public Map<String, String> loadDevNeoProperties()
    {
        final Map<String, String> devNeoMap = new LinkedHashMap<>();
        final Path path = configDirectory.resolve(DEV_NEO_FILE);
        if (!Files.exists(path))
        {
            return devNeoMap;
        }

        try
        {
            final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (final String line : lines)
            {
                final String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!"))
                {
                    continue;
                }
                final Matcher m = KEY_VALUE_PATTERN.matcher(line);
                if (m.find())
                {
                    devNeoMap.put(m.group(1).trim(), m.group(2).trim());
                }
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("Error loading {}", path, e);
        }

        return devNeoMap;
    }

    /**
     * Parses standard property file line by line extracting section titles, property keys,
     * values, descriptions, and active/commented states.
     */
    private List<PropertySectionDto> parsePropertyFile(final Path path, final Set<String> overriddenKeys,
            final Set<String> allKnownKeys)
    {
        final List<PropertySectionDto> sections = new ArrayList<>();
        if (!Files.exists(path))
        {
            return sections;
        }

        try
        {
            final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

            String currentSectionName = "General Configuration";
            List<PropertyEntryDto> currentEntries = new ArrayList<>();
            final List<String> currentCommentLines = new ArrayList<>();

            for (final String line : lines)
            {
                final String trimmed = line.trim();

                if (trimmed.isEmpty())
                {
                    continue;
                }

                if (trimmed.startsWith("#") || trimmed.startsWith("!"))
                {
                    final Matcher commentKeyMatcher = COMMENTED_KEY_PATTERN.matcher(line);
                    if (commentKeyMatcher.find())
                    {
                        final String key = commentKeyMatcher.group(1).trim();
                        final String value = commentKeyMatcher.group(2).trim();

                        allKnownKeys.add(key);

                        final String description = String.join("\n", currentCommentLines).trim();
                        currentCommentLines.clear();

                        final boolean overridden = overriddenKeys.contains(key);
                        final String inputType = determineInputType(key, value);

                        currentEntries.add(new PropertyEntryDto(key, value, description, false, overridden, inputType));
                        continue;
                    }

                    final String commentText = trimmed.replaceAll("^[#!]+\\s*", "").replaceAll("\\s*[#!]+$", "").trim();
                    if (commentText.startsWith("===") || commentText.startsWith("###") || commentText.startsWith("---"))
                    {
                        continue;
                    }

                    if (isSectionTitle(commentText))
                    {
                        if (!currentEntries.isEmpty())
                        {
                            sections.add(new PropertySectionDto(currentSectionName, currentEntries));
                            currentEntries = new ArrayList<>();
                        }
                        currentSectionName = commentText;
                        currentCommentLines.clear();
                    }
                    else if (!commentText.isEmpty())
                    {
                        currentCommentLines.add(commentText);
                    }
                    continue;
                }

                final Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(line);
                if (keyValueMatcher.find())
                {
                    final String key = keyValueMatcher.group(1).trim();
                    final String value = keyValueMatcher.group(2).trim();

                    allKnownKeys.add(key);

                    final String description = String.join("\n", currentCommentLines).trim();
                    currentCommentLines.clear();

                    final boolean overridden = overriddenKeys.contains(key);
                    final String inputType = determineInputType(key, value);

                    currentEntries.add(new PropertyEntryDto(key, value, description, true, overridden, inputType));
                }
            }

            if (!currentEntries.isEmpty())
            {
                sections.add(new PropertySectionDto(currentSectionName, currentEntries));
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to parse property file {}", path, e);
        }

        return sections;
    }

    /**
     * Parses browser.properties specifically grouping browser profiles into sections (e.g. browserprofile.global,
     * browserprofile.Chrome_headless, etc.).
     */
    private List<PropertySectionDto> parseBrowserPropertyFile(final Path path, final Set<String> overriddenKeys,
            final Set<String> allKnownKeys)
    {
        final Map<String, List<PropertyEntryDto>> profileMap = new LinkedHashMap<>();
        if (!Files.exists(path))
        {
            return List.of();
        }

        try
        {
            final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            final List<String> currentCommentLines = new ArrayList<>();

            for (final String line : lines)
            {
                final String trimmed = line.trim();
                if (trimmed.isEmpty())
                {
                    currentCommentLines.clear();
                    continue;
                }

                if (trimmed.startsWith("#") || trimmed.startsWith("!"))
                {
                    final String commentText = trimmed.replaceAll("^[#!]+\\s*", "").replaceAll("\\s*[#!]+$", "").trim();
                    if (!commentText.startsWith("===") && !commentText.startsWith("###") && !commentText.startsWith("---")
                            && !commentText.toLowerCase().contains("see also")
                            && !commentText.toLowerCase().contains("a browser profile")
                            && !commentText.toLowerCase().contains("mandatory properties")
                            && !commentText.toLowerCase().contains("global properties")
                            && !commentText.toLowerCase().contains("optional properties")
                            && !commentText.toLowerCase().contains("valid values")
                            && !commentText.isEmpty())
                    {
                        currentCommentLines.add(commentText);
                    }
                    continue;
                }

                final Matcher keyValueMatcher = KEY_VALUE_PATTERN.matcher(line);
                if (keyValueMatcher.find())
                {
                    final String key = keyValueMatcher.group(1).trim();
                    final String value = keyValueMatcher.group(2).trim();

                    if (!key.startsWith("browserprofile."))
                    {
                        currentCommentLines.clear();
                        continue;
                    }

                    allKnownKeys.add(key);

                    final String profileTag = extractProfileTag(key);
                    final String propName = extractPropName(key, profileTag);

                    String description = String.join("\n", currentCommentLines).trim();
                    currentCommentLines.clear();

                    if (description.length() > 120 || description.isEmpty()
                            || description.toLowerCase().contains("mandatory properties")
                            || description.toLowerCase().contains("optional properties")
                            || description.toLowerCase().contains("global properties")
                            || description.toLowerCase().contains("see also"))
                    {
                        description = getBrowserPropertyHelp(propName);
                    }

                    final boolean overridden = overriddenKeys.contains(key);
                    final String inputType = determineInputType(key, value);

                    profileMap.computeIfAbsent(profileTag, k -> new ArrayList<>())
                            .add(new PropertyEntryDto(key, value, description, true, overridden, inputType));
                }
            }
        }
        catch (final IOException e)
        {
            LOGGER.error("Failed to parse browser.properties {}", path, e);
        }

        // Guarantee 'global' default section is present
        profileMap.putIfAbsent("global", new ArrayList<>());

        final List<PropertySectionDto> result = new ArrayList<>();
        if (profileMap.containsKey("global"))
        {
            result.add(createPropertySectionDto("global", profileMap.get("global")));
        }

        for (final Map.Entry<String, List<PropertyEntryDto>> entry : profileMap.entrySet())
        {
            if (!"global".equalsIgnoreCase(entry.getKey()))
            {
                result.add(createPropertySectionDto(entry.getKey(), entry.getValue()));
            }
        }
        return result;
    }

    private String extractPropName(final String key, final String profileTag)
    {
        final String prefix = "browserprofile." + profileTag + ".";
        if (key.startsWith(prefix))
        {
            return key.substring(prefix.length());
        }
        return "";
    }

    /**
     * Returns a concise description and example for standard browser properties.
     */
    public static String getBrowserPropertyHelp(final String propName)
    {
        if (propName == null)
        {
            return "";
        }
        final String lower = propName.trim().toLowerCase();
        return switch (lower)
        {
            case "name" -> "Detailed title for this profile (e.g. 'Chrome 1024x768', 'Headless Firefox').";
            case "browser" -> "Target browser engine (e.g. 'chrome', 'firefox', 'safari', 'edge', 'iphone', 'android').";
            case "headless" -> "Run browser in headless mode without GUI window ('true' or 'false').";
            case "browserresolution" -> "Browser window dimensions in pixels (Width x Height, e.g. '1280x1024', '1920x1080').";
            case "arguments" -> "Command line flags for browser process chained with ';' (e.g. '--ignore-certificate-errors ; --disable-gpu').";
            case "driverargs" -> "Arguments for WebDriver binary process chained with ';' (e.g. '--log-level=INFO ; --port=7100').";
            case "preferences" -> "Browser user preferences chained with ';' (e.g. 'homepage=https://xceptance.com ; geolocation.enabled=true').";
            case "version" -> "Browser version or OS version for mobile emulation (e.g. '118.0', '12.0').";
            case "pageloadstrategy" -> "WebDriver page load strategy ('normal' = onload event, 'eager' = DOMContentLoaded, 'none').";
            case "acceptinsecurecertificates" -> "Automatically accept untrusted SSL certificates ('true' or 'false').";
            case "screenresolution" -> "Emulated operating system screen resolution (e.g. '1920x1080').";
            case "platform" -> "Target operating system platform (e.g. 'Windows 11', 'macOS 14', 'Linux').";
            case "deviceorientation" -> "Mobile device screen orientation ('portrait' or 'landscape').";
            case "testenvironment" -> "Execution target environment ('local' or 'saucelabs').";
            case "chromeemulationprofile" -> "Predefined Chrome mobile emulation profile (e.g. 'Samsung Galaxy S9', 'iPhone X').";
            case "downloaddirectory" -> "Default directory path for file downloads (e.g. 'target/downloads').";
            default -> "";
        };
    }


    private PropertySectionDto createPropertySectionDto(final String profileTag, final List<PropertyEntryDto> entries)
    {
        String browserType = "";
        final Set<String> existingPropNames = new HashSet<>();

        for (final PropertyEntryDto p : entries)
        {
            final String fullKey = p.getKey();
            final String prefix = "browserprofile." + profileTag + ".";
            if (fullKey.startsWith(prefix))
            {
                final String propName = fullKey.substring(prefix.length());
                existingPropNames.add(propName);

                if ("browser".equalsIgnoreCase(propName))
                {
                    browserType = p.getValue();
                }
            }
        }

        final String displayName = "global".equalsIgnoreCase(profileTag)
                ? "Global Browser Settings (browserprofile.global)"
                : "Profile: " + profileTag;

        final String icon = getBrowserIcon(browserType, profileTag);

        final List<String> expectedList = "global".equalsIgnoreCase(profileTag)
                ? EXPECTED_GLOBAL_PROPERTIES
                : EXPECTED_BROWSER_PROPERTIES;

        final List<String> missingExpectedProps = expectedList.stream()
                .filter(expected -> !existingPropNames.contains(expected))
                .toList();

        return new PropertySectionDto(displayName, profileTag, icon, entries, missingExpectedProps);
    }


    private String extractProfileTag(final String key)
    {
        if (key.startsWith("browserprofile."))
        {
            final String[] parts = key.split("\\.", 3);
            if (parts.length >= 2)
            {
                return parts[1];
            }
        }
        return "global";
    }

    private boolean isSectionTitle(final String text)
    {
        if (text.endsWith("properties") || text.endsWith("Properties") || text.endsWith("Configuration")
                || text.endsWith("Settings") || text.endsWith("Phase") || text.endsWith("Lighthouse")
                || text.endsWith("Authentication") || text.endsWith("Localization") || text.endsWith("utils")
                || text.endsWith("Utils") || text.endsWith("Blocker"))
        {
            return true;
        }
        return false;
    }

    private String determineInputType(final String key, final String value)
    {
        final String lowerKey = key.toLowerCase();
        if (lowerKey.contains("apikey") || lowerKey.contains("password") || lowerKey.contains("secret"))
        {
            return "password";
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))
        {
            return "boolean";
        }
        if (value.matches("^-?\\d+$"))
        {
            return "number";
        }
        return "text";
    }

    /**
     * Saves updated property values to a target property file while preserving original comments,
     * section titles, and line structure.
     */
    public synchronized void savePropertyFile(final String fileName, final Map<String, String> updatedProperties) throws IOException
    {
        final Path path = configDirectory.resolve(fileName);
        if (!Files.exists(path))
        {
            Files.createDirectories(configDirectory);
            Files.createFile(path);
        }

        final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        final List<String> newLines = new ArrayList<>();
        final Set<String> processedKeys = new HashSet<>();

        for (final String line : lines)
        {
            final Matcher activeMatcher = KEY_VALUE_PATTERN.matcher(line);
            if (activeMatcher.find())
            {
                final String key = activeMatcher.group(1).trim();
                if (updatedProperties.containsKey(key))
                {
                    final String newValue = updatedProperties.get(key);
                    newLines.add(key + " = " + (newValue != null ? newValue : ""));
                    processedKeys.add(key);
                    continue;
                }
            }

            final Matcher commentMatcher = COMMENTED_KEY_PATTERN.matcher(line);
            if (commentMatcher.find())
            {
                final String key = commentMatcher.group(1).trim();
                if (updatedProperties.containsKey(key) && updatedProperties.get(key) != null && !updatedProperties.get(key).trim().isEmpty())
                {
                    final String newValue = updatedProperties.get(key);
                    newLines.add(key + " = " + newValue);
                    processedKeys.add(key);
                    continue;
                }
            }

            newLines.add(line);
        }

        // Append any brand new keys that were not originally in the file
        for (final Map.Entry<String, String> entry : updatedProperties.entrySet())
        {
            final String key = entry.getKey();
            if (!processedKeys.contains(key) && entry.getValue() != null && !entry.getValue().trim().isEmpty())
            {
                newLines.add(key + " = " + entry.getValue());
            }
        }

        Files.write(path, newLines, StandardCharsets.UTF_8);
    }

    /**
     * Saves dev-neodymium.properties with key-value entries.
     */
    public synchronized void saveDevNeoProperties(final List<Map.Entry<String, String>> devNeoEntries) throws IOException
    {
        final Path path = configDirectory.resolve(DEV_NEO_FILE);
        Files.createDirectories(configDirectory);

        final List<String> lines = new ArrayList<>();
        lines.add("# Temporary local override properties (dev-neodymium.properties)");
        lines.add("# These properties take precedence over all standard property files.");
        lines.add("");

        for (final Map.Entry<String, String> entry : devNeoEntries)
        {
            if (entry.getKey() != null && !entry.getKey().trim().isEmpty())
            {
                lines.add(entry.getKey().trim() + "=" + (entry.getValue() != null ? entry.getValue().trim() : ""));
            }
        }

        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    /**
     * Appends a new browser profile to browser.properties.
     */
    public synchronized void addBrowserProfile(final String profileName, final String browserType) throws IOException
    {
        final Path path = configDirectory.resolve(BROWSER_FILE);
        final String sanitizedName = profileName.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        final String cleanBrowser = (browserType != null && !browserType.trim().isEmpty()) ? browserType.trim().toLowerCase() : "chrome";

        final List<String> newLines = new ArrayList<>();
        newLines.add("");
        newLines.add("# Custom Browser Profile: " + profileName.trim());
        newLines.add("browserprofile." + sanitizedName + ".name = " + profileName.trim());
        newLines.add("browserprofile." + sanitizedName + ".browser = " + cleanBrowser);
        newLines.add("browserprofile." + sanitizedName + ".headless = true");

        if (Files.exists(path))
        {
            Files.write(path, newLines, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
        }
        else
        {
            Files.write(path, newLines, StandardCharsets.UTF_8);
        }
    }

    /**
     * Adds a new property to a specific browser profile in browser.properties.
     */
    public synchronized void addBrowserProperty(final String profileTag, final String propertyName, final String propertyValue) throws IOException
    {
        final Path path = configDirectory.resolve(BROWSER_FILE);
        final String cleanTag = profileTag.trim();
        final String cleanProp = propertyName.trim();
        final String fullKey = "browserprofile." + cleanTag + "." + cleanProp;

        final Map<String, String> map = new HashMap<>();
        map.put(fullKey, propertyValue != null ? propertyValue.trim() : "");
        savePropertyFile(BROWSER_FILE, map);
    }
}



