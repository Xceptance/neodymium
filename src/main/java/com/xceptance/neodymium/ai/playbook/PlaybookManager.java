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
package com.xceptance.neodymium.ai.playbook;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.PropertiesUtil;

/**
 * Manages the persistence lifecycle of execution {@link Playbook}s.
 * Handles loading playbooks from JSON files, saving recorded sessions back to disk,
 * and dynamically mapping playbook IDs to hierarchical filesystem locations.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookManager
{
    private static final Logger LOG = LoggerFactory.getLogger(PlaybookManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PlaybookManager()
    {
        // Prevent instantiation of utility class
    }

    /**
     * Loads a playbook from its corresponding JSON persistence file.
     *
     * @param id the unique identifier of the playbook to load
     * @return the loaded {@link Playbook} instance, or {@code null} if the file does not exist or failed to load
     */
    public static Playbook loadPlaybook(final String id)
    {
        final File file = getPlaybookFile(id);
        if (file.exists())
        {
            try (final FileReader reader = new FileReader(file))
            {
                final Playbook playbook = GSON.fromJson(reader, Playbook.class);
                playbook.markActionsReplay();
                playbook.setId(id);
                LOG.info("======== 📖 Playbook Loaded: {} ========", file.getPath());
                return playbook;
            }
            catch (final Exception e)
            {
                LOG.error("Failed to load playbook: {}", file.getAbsolutePath(), e);
            }
        }
        return null;
    }

    /**
     * Saves a playbook session back to its JSON persistence file.
     * Respects the configured recording policies and skips saving if recording is disabled.
     *
     * @param playbook the {@link Playbook} instance to save
     */
    public static void savePlaybook(final Playbook playbook)
    {
        if (playbook == null || playbook.getId() == null)
        {
            return;
        }

        if (!Neodymium.aiConfiguration().playbookRecordEnabled())
        {
            LOG.info("Playbook recording is disabled. Skipping save for {}", playbook.getId());
            return;
        }

        final File file = getPlaybookFile(playbook.getId());
        try
        {
            Files.createDirectories(file.getParentFile().toPath());
            try (final FileWriter writer = new FileWriter(file))
            {
                GSON.toJson(playbook, writer);
                LOG.info("======== 💾 Playbook Saved: {} ========", file.getPath());
            }
        }
        catch (final IOException e)
        {
            LOG.error("Failed to save playbook: {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Maps a logical playbook ID to its absolute filesystem file path.
     * Translates package qualifiers (dots) into directory paths, separates test classes
     * from methods, and sanitizes characters for safe cross-platform file storage.
     *
     * @param playbookId the logical playbook identifier
     * @return the resolved {@link File} pointer
     */
    public static File getPlaybookFile(final String playbookId)
    {
        final String resolvedId = playbookId == null ? "unknown" : playbookId;

        final String pathPart;
        final String filePart;

        final int separatorIndex = resolvedId.indexOf(" :: ");
        if (separatorIndex != -1)
        {
            final String rawPathPart = resolvedId.substring(0, separatorIndex);
            final String rawFilePart = resolvedId.substring(separatorIndex + 4);

            // Clean up both parts independently to remove illegal filesystem characters.
            pathPart = rawPathPart.replaceAll("[^_a-zA-Z0-9.-]", "_").trim().replaceAll("_+", "_");
            filePart = rawFilePart.replaceAll("[^_a-zA-Z0-9.-]", "_").replace("Browser", "").trim().replaceAll("_+", "_");
        }
        else
        {
            final String cleanId = resolvedId.replaceAll("[^_a-zA-Z0-9.-]", "_").replace("Browser", "").trim().replaceAll("_+", "_");
            final int lastDotIndex = cleanId.lastIndexOf('.');
            final int underscoreIndex = cleanId.indexOf('_', lastDotIndex);

            if (underscoreIndex == -1)
            {
                if (lastDotIndex != -1)
                {
                    pathPart = cleanId.substring(0, lastDotIndex);
                    filePart = cleanId.substring(lastDotIndex + 1);
                }
                else
                {
                    pathPart = "";
                    filePart = cleanId;
                }
            }
            else
            {
                pathPart = cleanId.substring(0, underscoreIndex);
                filePart = cleanId.substring(underscoreIndex + 1);
            }
        }

        // Clean up leading/trailing underscores that might have been left over after replacements.
        final String sanitizedFilePart = filePart.replaceAll("^_|_$", "");

        // Convert path: replace package dots with filesystem directory slashes
        final String directoryPath = pathPart.replace('.', '/');

        // Resolve playbook directory dynamically
        final String playbookDir = getPlaybookDirectory(playbookId);

        // Build file references pointing to the persistence root
        final String fileName = sanitizedFilePart + ".json";
        if (!directoryPath.isEmpty())
        {
            return new File(new File(playbookDir, directoryPath), fileName);
        }
        else
        {
            return new File(playbookDir, fileName);
        }
    }

    private static String getPlaybookDirectory(final String playbookId)
    {
        if (playbookId == null)
        {
            return getPlaybookDirectoryProperty("playbook.directory.global", Neodymium.aiConfiguration().playbookDirectoryGlobal());
        }

        final String cleanIdWithSpaces = playbookId.trim();
        final String cleanIdNoSpaces = cleanIdWithSpaces.replace(" :: ", "::");

        // Try method level key
        String dir = getPlaybookDirectoryProperty("playbook.directory.method." + cleanIdNoSpaces, null);
        if (dir != null)
        {
            return dir;
        }
        dir = getPlaybookDirectoryProperty("playbook.directory.method." + cleanIdWithSpaces, null);
        if (dir != null)
        {
            return dir;
        }

        // Try class key with method suffix for backwards-compatibility/flexibility
        dir = getPlaybookDirectoryProperty("playbook.directory.class." + cleanIdNoSpaces, null);
        if (dir != null)
        {
            return dir;
        }
        dir = getPlaybookDirectoryProperty("playbook.directory.class." + cleanIdWithSpaces, null);
        if (dir != null)
        {
            return dir;
        }

        // Extract class name
        String className = null;
        final int separatorIndex = cleanIdWithSpaces.indexOf(" :: ");
        if (separatorIndex != -1)
        {
            className = cleanIdWithSpaces.substring(0, separatorIndex).trim();
        }
        else
        {
            final int lastDot = cleanIdWithSpaces.lastIndexOf('.');
            if (lastDot != -1)
            {
                className = cleanIdWithSpaces.substring(0, lastDot).trim();
            }
        }

        // Try class keys
        if (className != null)
        {
            dir = getPlaybookDirectoryProperty("playbook.directory.class." + className, null);
            if (dir != null)
            {
                return dir;
            }
        }

        // Try package keys (traversing up package segments)
        if (className != null)
        {
            final int lastDotOfClass = className.lastIndexOf('.');
            if (lastDotOfClass != -1)
            {
                String packageName = className.substring(0, lastDotOfClass);
                while (packageName != null && !packageName.isEmpty())
                {
                    dir = getPlaybookDirectoryProperty("playbook.directory.package." + packageName, null);
                    if (dir != null)
                    {
                        return dir;
                    }

                    final int dotIdx = packageName.lastIndexOf('.');
                    if (dotIdx != -1)
                    {
                        packageName = packageName.substring(0, dotIdx);
                    }
                    else
                    {
                        packageName = null;
                    }
                }
            }
        }

        // Fallback to global
        return getPlaybookDirectoryProperty("playbook.directory.global", Neodymium.aiConfiguration().playbookDirectoryGlobal());
    }

    private static String getPlaybookDirectoryProperty(final String key, final String defaultValue)
    {
        // 1. Check thread-local test data
        if (Neodymium.getData().exists(key))
        {
            final String val = Neodymium.getData().get(key);
            if (val != null && !val.trim().isEmpty())
            {
                return val.endsWith("/") ? val : val + "/";
            }
        }

        // 2. Check System properties
        final String systemProp = System.getProperty(key);
        if (systemProp != null && !systemProp.trim().isEmpty())
        {
            return systemProp.endsWith("/") ? systemProp : systemProp + "/";
        }

        // 3. Check loaded properties in config/ai.properties
        final Properties aiProps = PropertiesUtil.loadPropertiesFromFile("config/ai.properties");
        if (aiProps.containsKey(key))
        {
            final String val = aiProps.getProperty(key);
            if (val != null && !val.trim().isEmpty())
            {
                return val.endsWith("/") ? val : val + "/";
            }
        }

        // 4. Check loaded properties in config/neodymium.properties
        final Properties neoProps = PropertiesUtil.loadPropertiesFromFile("config/neodymium.properties");
        if (neoProps.containsKey(key))
        {
            final String val = neoProps.getProperty(key);
            if (val != null && !val.trim().isEmpty())
            {
                return val.endsWith("/") ? val : val + "/";
            }
        }

        return defaultValue == null ? null : (defaultValue.endsWith("/") ? defaultValue : defaultValue + "/");
    }
}

