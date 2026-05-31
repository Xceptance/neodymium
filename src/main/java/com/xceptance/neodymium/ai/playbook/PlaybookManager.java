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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Manages the persistence lifecycle of execution {@link Playbook}s.
 * Handles loading playbooks from JSON files, saving recorded sessions back to disk,
 * and dynamically mapping playbook IDs to hierarchical filesystem locations.
 *
 * @author AI-generated: Gemini 2.5 Flash
 */
public final class PlaybookManager
{
    private static final Logger LOG = LoggerFactory.getLogger(PlaybookManager.class);
    private static final String PLAYBOOK_DIR = "src/test/resources/ai-playbooks/";
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

        // Build file references pointing to the persistence root
        final String fileName = sanitizedFilePart + ".json";
        if (!directoryPath.isEmpty())
        {
            return new File(new File(PLAYBOOK_DIR, directoryPath), fileName);
        }
        else
        {
            return new File(PLAYBOOK_DIR, fileName);
        }
    }
}
