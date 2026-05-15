/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
  *
 * // AI-generated: Gemini 2.0 Flash
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

        playbookId = playbookId.replaceAll("[^_a-zA-Z0-9.-]", "_").replace("Browser", "").trim().replaceAll("_+", "_");

        // Find last dot
        int lastDotIndex = playbookId.lastIndexOf('.');

        // Find first underscore after last dot
        int underscoreIndex = playbookId.indexOf('_', lastDotIndex);

        // Split into path part and filename part
        String pathPart;
        String filePart;
        if (underscoreIndex == -1) {
            if (lastDotIndex != -1) {
                pathPart = playbookId.substring(0, lastDotIndex);
                filePart = playbookId.substring(lastDotIndex + 1);
            } else {
                pathPart = "";
                filePart = playbookId;
            }
        } else {
            pathPart = playbookId.substring(0, underscoreIndex);
            filePart = playbookId.substring(underscoreIndex + 1);
        }

        // Convert path: replace dots with slashes
        String path = pathPart.replace('.', '/');

        // Build filename
        String fileName = filePart + ".json";
        if (!path.isEmpty()) {
            LOG.debug("Playbook target path: {}", path + "/" + fileName);
            return new File(new File(PLAYBOOK_DIR, path), fileName);
        } else {
            LOG.debug("Playbook target path: {}", fileName);
            return new File(PLAYBOOK_DIR, fileName);
        }
    }
}
