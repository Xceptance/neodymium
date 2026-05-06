package com.xceptance.neodymium.ai.generator;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.core.HudActionException;
import com.xceptance.neodymium.ai.core.HudActionType;
import com.xceptance.neodymium.util.Neodymium;

public class InteractiveHud {

    private static final Logger LOG = LoggerFactory.getLogger(InteractiveHud.class);

    private static final String HUD_JS;
    private static final String HUD_HTML;

    static {
        HUD_JS = loadResource("hud.js");
        HUD_HTML = loadResource("hud.html");
    }

    private static String loadResource(String filename) {
        try (java.io.InputStream is = InteractiveHud.class.getResourceAsStream("/com/xceptance/neodymium/ai/generator/" + filename)) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOG.error("Failed to load HUD resource: " + filename, e);
        }
        return "";
    }

    private String sourceFile;
    private Map<String, String> dataBindings;
    private boolean canEdit;
    
    private List<String> lastPlanned;
    private List<String> lastPerformed;
    private boolean lastAutoSkip;
    private boolean lastHudPromptChanged;
    private boolean lastIsFinished;
    private String lastCurrentUnresolvedStep;
    private String lastReasoning;
    private boolean lastIsReplay;

    public InteractiveHud() {
        evaluateCanEdit();
    }

    private void evaluateCanEdit() {
        try {
            if (com.xceptance.neodymium.util.Neodymium.getData() != null) {
                this.sourceFile = com.xceptance.neodymium.util.Neodymium.getData().exists("neodymium.sourceFile") ? com.xceptance.neodymium.util.Neodymium.getData().asString("neodymium.sourceFile") : null;
            }
        } catch (Exception e) {
            // ignore if no data
        }

        if (this.sourceFile == null) {
            this.canEdit = false;
        } else {
            String lower = this.sourceFile.toLowerCase();
            this.canEdit = lower.endsWith(".yml") || lower.endsWith(".yaml");
        }
        if (!this.canEdit) {
            String keys = "";
            try {
                if (com.xceptance.neodymium.util.Neodymium.getData() != null) {
                    keys = com.xceptance.neodymium.util.Neodymium.getData().keySet().toString();
                }
            } catch (Exception e) {}
            LOG.warn("Interactive HUD initialized in READ-ONLY mode. The test data source is not a YAML file (or is null). Editing steps or data is disabled. Data keys available: " + keys);
        }
    }
    private Map<String, String> originalDataBindings;
    
    public void setDataBindings(Map<String, String> dataBindings) {
        this.dataBindings = dataBindings;
        if (this.originalDataBindings == null) {
            this.originalDataBindings = new java.util.HashMap<>(dataBindings);
        }
    }

    public boolean canEdit() {
        evaluateCanEdit();
        return canEdit;
    }

    public void injectOrUpdateHud(List<String> planned, List<String> performed, boolean autoSkip,
            boolean hudPromptChanged, boolean isFinished, String currentUnresolvedStep, String reasoning, boolean isReplay) {
        this.lastPlanned = planned;
        this.lastPerformed = performed;
        this.lastAutoSkip = autoSkip;
        this.lastHudPromptChanged = hudPromptChanged;
        this.lastIsFinished = isFinished;
        this.lastCurrentUnresolvedStep = currentUnresolvedStep;
        this.lastReasoning = reasoning;
        this.lastIsReplay = isReplay;

        evaluateCanEdit();
        Map<String, String> configMap = new java.util.HashMap<>();
        if (com.xceptance.neodymium.util.Neodymium.configuration() instanceof org.aeonbits.owner.Accessible) {
            org.aeonbits.owner.Accessible conf = (org.aeonbits.owner.Accessible) com.xceptance.neodymium.util.Neodymium.configuration();
            java.util.Set<String> props = conf.propertyNames();
            if (props != null) {
                for (String p : props) {
                    configMap.put(p, conf.getProperty(p));
                }
            }
        }
        try {
            com.codeborne.selenide.Selenide.executeJavaScript(HUD_JS, HUD_HTML, planned, performed, autoSkip,
                    hudPromptChanged, isFinished, canEdit, currentUnresolvedStep, dataBindings, configMap, reasoning, isReplay);
        } catch (Exception e) {
            LOG.warn("Failed to inject AI Generation HUD: {}", e.getMessage());
        }
    }

    public void injectOrUpdateHud(List<String> planned, List<String> performed, boolean autoSkip,
            boolean hudPromptChanged, boolean isFinished, String currentUnresolvedStep) {
        injectOrUpdateHud(planned, performed, autoSkip, hudPromptChanged, isFinished, currentUnresolvedStep, null, false);
    }

    public String checkHudAction() {
        try {
            Boolean hudExists = com.codeborne.selenide.Selenide.executeJavaScript("return document.getElementById('neo-ai-hud') !== null;");
            if (Boolean.FALSE.equals(hudExists) && lastPlanned != null) {
                // The HUD was removed (e.g. page navigation), reinject it silently.
                injectOrUpdateHud(lastPlanned, lastPerformed, lastAutoSkip, lastHudPromptChanged, lastIsFinished, lastCurrentUnresolvedStep, lastReasoning, lastIsReplay);
            } else if (Boolean.TRUE.equals(hudExists)) {
                // If it exists, sync autoSkip state in case user changed it before navigating
                Object skipStatus = com.codeborne.selenide.Selenide.executeJavaScript("return window.neoHudAutoSkip;");
                if (skipStatus != null) {
                    this.lastAutoSkip = (Boolean) skipStatus;
                }
            }

            Object status = com.codeborne.selenide.Selenide.executeJavaScript("return window.neoHudAction;");
            if (status != null) {
                return String.valueOf(status);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean checkAutoSkipStatus() {
        try {
            Object status = com.codeborne.selenide.Selenide.executeJavaScript("return window.neoHudAutoSkip;");
            if (status == null)
                return null;
            return (Boolean) status;
        } catch (Exception e) {
            return null;
        }
    }

    public void resetHudAction() {
        try {
            com.codeborne.selenide.Selenide.executeJavaScript("window.neoHudAction = null;");
        } catch (Exception e) {
            // ignore
        }
    }

    public void saveYamlDataFileIfModified(List<String> performedInstructions) {
        if (!canEdit || sourceFile == null) {
            LOG.info("Skip saving YAML: canEdit is false or sourceFile is null.");
            return;
        }

        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Object data = null;
            java.io.File file = new java.io.File(sourceFile);
            
            // Handle relative path if it doesn't exist
            if (!file.exists() && !sourceFile.startsWith("src/test/resources/")) {
                file = new java.io.File("src/test/resources/" + (sourceFile.startsWith("/") ? sourceFile.substring(1) : sourceFile));
            }

            if (file.exists()) {
                try (java.io.InputStream is = new java.io.FileInputStream(file)) {
                    data = yaml.load(is);
                }
            }

            String newPrompt = String.join("\n", performedInstructions) + "\n";

            Map<String, Object> targetDataset = null;

            if (data instanceof Map) {
                Map<String, Object> root = (Map<String, Object>) data;
                root.put("prompt", newPrompt);
                
                if (root.containsKey("data")) {
                    targetDataset = findTargetDataset(root.get("data"), originalDataBindings);
                } else {
                    targetDataset = root;
                }
            } else if (data instanceof List) {
                targetDataset = findTargetDataset(data, originalDataBindings);
                
                Map<String, Object> root = new java.util.LinkedHashMap<>();
                root.put("prompt", newPrompt);
                root.put("data", data);
                data = root;
            } else {
                Map<String, Object> root = new java.util.LinkedHashMap<>();
                root.put("prompt", newPrompt);
                data = root;
            }

            if (targetDataset != null && this.dataBindings != null) {
                for (Map.Entry<String, String> entry : this.dataBindings.entrySet()) {
                    if (!entry.getKey().startsWith("neodymium.") && !entry.getKey().equals("prompt")) {
                        targetDataset.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
            options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            org.yaml.snakeyaml.Yaml writerYaml = new org.yaml.snakeyaml.Yaml(options);
            
            java.nio.file.Path parentDir = file.toPath().getParent();
            if (parentDir != null && !java.nio.file.Files.exists(parentDir)) {
                java.nio.file.Files.createDirectories(parentDir);
            }

            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writerYaml.dump(data, writer);
            }
            LOG.info("💾 Successfully updated YAML data file: {}", file.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("Failed to save YAML data file", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findTargetDataset(Object yamlData, Map<String, String> originalBindings) {
        if (originalBindings == null) return null;
        
        if (yamlData instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) yamlData;
            if (map.containsKey("data")) {
                return findTargetDataset(map.get("data"), originalBindings);
            }
            return map;
        } else if (yamlData instanceof List) {
            List<?> list = (List<?>) yamlData;
            
            // 1. Match by testId
            String testId = originalBindings.get("testId") != null ? originalBindings.get("testId") : originalBindings.get("TEST_ID");
            if (testId != null) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) item;
                        String tId = map.get("testId") != null ? String.valueOf(map.get("testId")) : (map.get("TEST_ID") != null ? String.valueOf(map.get("TEST_ID")) : null);
                        if (testId.equals(tId)) {
                            return map;
                        }
                    }
                }
            }
            
            // 2. Exact match of non-neodymium keys
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    boolean match = true;
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        String key = entry.getKey();
                        if (key.startsWith("neodymium.")) continue;
                        if (!originalBindings.containsKey(key)) {
                            match = false; break;
                        }
                        String originalVal = originalBindings.get(key);
                        String yamlVal = String.valueOf(entry.getValue());
                        if (!yamlVal.equals(originalVal)) {
                            match = false; break;
                        }
                    }
                    if (match) return map;
                }
            }
            
            // 3. Fallback to first item if it's the only one
            if (list.size() == 1 && list.get(0) instanceof Map) {
                return (Map<String, Object>) list.get(0);
            }
        }
        return null;
    }
}
