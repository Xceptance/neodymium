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
                LOG.info("Playbook saved to {}", file.getAbsolutePath());
            }
        } catch (IOException e) {
            LOG.error("Failed to save playbook: {}", file.getAbsolutePath(), e);
        }
    }

    private static File getPlaybookFile(String playbookId) {
        if (playbookId == null) {
            playbookId = "unknown";
        }

        playbookId = playbookId.replaceAll("[^_a-zA-Z0-9.-]", "_").replace("Browser", "").trim().replaceAll("_+", "_");

        // Find last dot
        int lastDotIndex = playbookId.lastIndexOf('.');

        // Find first underscore after last dot
        int underscoreIndex = playbookId.indexOf('_', lastDotIndex);

        // Split into path part and filename part
        String pathPart = playbookId.substring(0, underscoreIndex);
        String filePart = playbookId.substring(underscoreIndex + 1);

        // Convert path: replace dots with slashes
        String path = pathPart.replace('.', '/');

        // Build filename
        String fileName = filePart + ".json";
        if (!path.isEmpty()) {
            LOG.info("Playbook file path calculated as: {}", path + "/" + fileName);
            return new File(new File(PLAYBOOK_DIR, path), fileName);
        } else {
            LOG.info("Playbook file path calculated as: {}", fileName);
            return new File(PLAYBOOK_DIR, fileName);
        }
    }
}
