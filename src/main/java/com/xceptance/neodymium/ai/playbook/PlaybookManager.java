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

public class PlaybookManager {
    private static final Logger LOG = LoggerFactory.getLogger(PlaybookManager.class);

    private static final String PLAYBOOK_DIR = "src/test/resources/ai-playbooks/";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Playbook loadPlaybook(String id) {
        File file = getPlaybookFile(id);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Playbook playbook = GSON.fromJson(reader, Playbook.class);
                playbook.markActionsReplay();
                playbook.setId(id);
                LOG.info("======== 📖 Playbook Loaded: {} ========", file.getPath());
                return playbook;
            } catch (Exception e) {
                LOG.error("Failed to load playbook: {}", file.getAbsolutePath(), e);
            }
        }
        return null;
    }

    public static void savePlaybook(Playbook playbook) {
        if (playbook == null || playbook.getId() == null) {
            return;
        }

        if (!Neodymium.aiConfiguration().playbookRecordEnabled()) {
            LOG.info("Playbook recording is disabled. Skipping save for {}", playbook.getId());
            return;
        }

        File file = getPlaybookFile(playbook.getId());
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(playbook, writer);
                LOG.info("======== 💾 Playbook Saved: {} ========", file.getPath());
            }
        } catch (IOException e) {
            LOG.error("Failed to save playbook: {}", file.getAbsolutePath(), e);
        }
    }

    public static File getPlaybookFile(String playbookId) {
        if (playbookId == null) {
            playbookId = "unknown";
        }

        String pathPart;
        String filePart;

        int separatorIndex = playbookId.indexOf(" :: ");
        if (separatorIndex != -1) {
            pathPart = playbookId.substring(0, separatorIndex);
            filePart = playbookId.substring(separatorIndex + 4);

            // Clean up both parts independently
            pathPart = pathPart.replaceAll("[^_a-zA-Z0-9.-]", "_").trim().replaceAll("_+", "_");
            filePart = filePart.replaceAll("[^_a-zA-Z0-9.-]", "_").replace("Browser", "").trim().replaceAll("_+", "_");
        } else {
            String cleanId = playbookId.replaceAll("[^_a-zA-Z0-9.-]", "_").replace("Browser", "").trim().replaceAll("_+", "_");
            int lastDotIndex = cleanId.lastIndexOf('.');
            int underscoreIndex = cleanId.indexOf('_', lastDotIndex);

            if (underscoreIndex == -1) {
                if (lastDotIndex != -1) {
                    pathPart = cleanId.substring(0, lastDotIndex);
                    filePart = cleanId.substring(lastDotIndex + 1);
                } else {
                    pathPart = "";
                    filePart = cleanId;
                }
            } else {
                pathPart = cleanId.substring(0, underscoreIndex);
                filePart = cleanId.substring(underscoreIndex + 1);
            }
        }

        // Remove trailing or leading underscores from filePart that might have been left over
        filePart = filePart.replaceAll("^_|_$", "");

        // Convert path: replace dots with slashes
        String path = pathPart.replace('.', '/');

        // Build filename
        String fileName = filePart + ".json";
        if (!path.isEmpty()) {
            return new File(new File(PLAYBOOK_DIR, path), fileName);
        } else {
            return new File(PLAYBOOK_DIR, fileName);
        }
    }
}
