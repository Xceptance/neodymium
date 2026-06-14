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
package com.xceptance.neodymium.ai.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aeonbits.owner.Accessible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import com.codeborne.selenide.Selenide;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.xceptance.neodymium.ai.core.HudActionException;
import com.xceptance.neodymium.ai.core.HudActionType;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Manages the client-side interactive Heads-Up Display (HUD) injected into the active browser page.
 * Enables live test playback reviews, manual interventions, breakpoint triggers, and step skips,
 * while automatically parsing, synchronizing, and saving modifications back to the original YAML test data.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class InteractiveHud
{
    private static final Logger LOG = LoggerFactory.getLogger(InteractiveHud.class);

    private static final String HUD_JS;
    private static final String HUD_HTML;

    static
    {
        HUD_JS = loadResource("hud.js");
        HUD_HTML = loadResource("hud.html");
    }

    /**
     * Loads a text resource relative to this class's classpath package.
     *
     * @param filename the name of the resource file to load
     * @return the text contents of the resource, or an empty string if it failed to load
     */
    private static String loadResource(final String filename)
    {
        try (final InputStream is = InteractiveHud.class.getResourceAsStream("/com/xceptance/neodymium/ai/generator/" + filename))
        {
            if (is != null)
            {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        catch (final Exception e)
        {
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
    private boolean lastFullPromptOpen = false;
    private String lastBreakpointsStr = "[]";
    private boolean lastHelpShown = false;
    private Map<String, String> originalDataBindings;

    /**
     * Constructs a new InteractiveHud and evaluates if the current test context supports edit permissions.
     */
    public InteractiveHud()
    {
        evaluateCanEdit();
    }

    /**
     * Evaluates if the current execution has edit permissions based on the active test data source.
     * Only supports editing if the data source is a YAML/YML file.
     */
    private void evaluateCanEdit()
    {
        try
        {
            if (Neodymium.getData() != null)
            {
                this.sourceFile = Neodymium.getData().exists("neodymium.sourceFile") ? Neodymium.getData().asString("neodymium.sourceFile") : null;
            }
        }
        catch (final Exception e)
        {
            // ignore if no data is present
        }

        if (this.sourceFile == null)
        {
            this.canEdit = false;
        }
        else
        {
            final String lower = this.sourceFile.toLowerCase();
            this.canEdit = lower.endsWith(".yml") || lower.endsWith(".yaml");
        }
        if (!this.canEdit)
        {
            String keys = "";
            try
            {
                if (Neodymium.getData() != null)
                {
                    keys = Neodymium.getData().keySet().toString();
                }
            }
            catch (final Exception e)
            {
                // ignore if data key inspection fails
            }
            LOG.warn("Interactive HUD initialized in READ-ONLY mode. The test data source is not a YAML file (or is null). Editing steps or data is disabled. Data keys available: " + keys);
        }
    }

    /**
     * Configures the dataset variable bindings from the active data iteration.
     * Preserves the original bindings to accurately locate the matching row in data files.
     *
     * @param dataBindings the active map of key-value data bindings
     */
    public void setDataBindings(final Map<String, String> dataBindings)
    {
        this.dataBindings = dataBindings;
        if (this.originalDataBindings == null && dataBindings != null)
        {
            this.originalDataBindings = new HashMap<>(dataBindings);
        }
    }

    /**
     * Checks if the HUD has edit permissions (e.g. is backed by a writable YAML file).
     *
     * @return {@code true} if edit operations are supported, {@code false} if read-only
     */
    public boolean canEdit()
    {
        evaluateCanEdit();
        return this.canEdit;
    }

    /**
     * Injects or updates the HUD state inside the active browser page via Selenide JavaScript execution.
     *
     * @param planned                   the list of planned future instructions
     * @param performed                 the list of completed/executed instructions
     * @param autoSkip                  the auto-skip status
     * @param hudPromptChanged          indicates if an instruction was modified inside the HUD
     * @param isFinished                indicates if the AI has completed execution
     * @param currentUnresolvedStep     the prompt of the step currently undergoing execution
     * @param reasoning                 the AI's natural language reasoning trace for the current step
     * @param isReplay                  indicates if we are in replay mode or live exploration mode
     */
    public void injectOrUpdateHud(final List<String> planned, final List<String> performed, final boolean autoSkip,
            final boolean hudPromptChanged, final boolean isFinished, final String currentUnresolvedStep, final String reasoning, final boolean isReplay)
    {
        this.lastPlanned = planned;
        this.lastPerformed = performed;
        this.lastAutoSkip = autoSkip;
        this.lastHudPromptChanged = hudPromptChanged;
        this.lastIsFinished = isFinished;
        this.lastCurrentUnresolvedStep = currentUnresolvedStep;
        this.lastReasoning = reasoning;
        this.lastIsReplay = isReplay;

        evaluateCanEdit();
        final Map<String, String> configMap = new HashMap<>();
        if (Neodymium.configuration() instanceof Accessible)
        {
            final Accessible AccessibleConf = (Accessible) Neodymium.configuration();
            final Set<String> props = AccessibleConf.propertyNames();
            if (props != null)
            {
                for (final String p : props)
                {
                    configMap.put(p, AccessibleConf.getProperty(p));
                }
            }
        }
        try
        {
            Selenide.executeJavaScript(HUD_JS, HUD_HTML, planned, performed, autoSkip,
                    hudPromptChanged, isFinished, this.canEdit, currentUnresolvedStep, this.dataBindings, configMap, reasoning, isReplay, this.lastFullPromptOpen, this.lastBreakpointsStr, this.lastHelpShown);
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to inject AI Generation HUD: {}", e.getMessage());
        }
    }

    /**
     * Injects or updates the HUD state inside the active browser page with default values for reasoning/replay.
     *
     * @param planned               the list of planned future instructions
     * @param performed             the list of completed/executed instructions
     * @param autoSkip              the auto-skip status
     * @param hudPromptChanged      indicates if an instruction was modified inside the HUD
     * @param isFinished            indicates if the AI has completed execution
     * @param currentUnresolvedStep the prompt of the step currently undergoing execution
     */
    public void injectOrUpdateHud(final List<String> planned, final List<String> performed, final boolean autoSkip,
            final boolean hudPromptChanged, final boolean isFinished, final String currentUnresolvedStep)
    {
        injectOrUpdateHud(planned, performed, autoSkip, hudPromptChanged, isFinished, currentUnresolvedStep, null, false);
    }

    /**
     * Checks if the user has performed a state-changing action in the HUD.
     * Automatically handles silent reinjections if page transitions or reloads destroyed the DOM.
     *
     * @return the JSON action string representation if an action was clicked, or {@code null} if idle
     */
    public String checkHudAction()
    {
        try
        {
            final Boolean hudExists = Selenide.executeJavaScript("return document.getElementById('neo-ai-hud') !== null;");
            if (Boolean.FALSE.equals(hudExists) && this.lastPlanned != null)
            {
                // The HUD was removed (e.g. page navigation), reinject it silently.
                injectOrUpdateHud(this.lastPlanned, this.lastPerformed, this.lastAutoSkip, this.lastHudPromptChanged, this.lastIsFinished, this.lastCurrentUnresolvedStep, this.lastReasoning, this.lastIsReplay);
            }
            else if (Boolean.TRUE.equals(hudExists))
            {
                // If it exists, sync HUD interaction variables back into the local state
                final Object skipStatus = Selenide.executeJavaScript("return window.neoHudAutoSkip;");
                if (skipStatus != null)
                {
                    this.lastAutoSkip = (Boolean) skipStatus;
                }
                final Object promptOpen = Selenide.executeJavaScript("return window.neoFullPromptOpen;");
                if (promptOpen != null)
                {
                    this.lastFullPromptOpen = (Boolean) promptOpen;
                }
                
                final Object bps = Selenide.executeJavaScript("return window.neoBreakpoints ? JSON.stringify(window.neoBreakpoints) : null;");
                if (bps != null)
                {
                    this.lastBreakpointsStr = (String) bps;
                }
                
                final Object helpShown = Selenide.executeJavaScript("return window.neoHelpShown;");
                if (helpShown != null)
                {
                    this.lastHelpShown = (Boolean) helpShown;
                }
            }

            final Object status = Selenide.executeJavaScript("var val = window.neoHudAction; window.neoHudAction = null; return val;");
            if (status != null)
            {
                return String.valueOf(status);
            }
            return null;
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    /**
     * Checks the auto-skip status toggled in the HUD window.
     *
     * @return the auto-skip state, or {@code null} if inspection failed
     */
    public Boolean checkAutoSkipStatus()
    {
        try
        {
            final Object status = Selenide.executeJavaScript("return window.neoHudAutoSkip;");
            if (status == null)
            {
                return null;
            }
            return (Boolean) status;
        }
        catch (final Exception e)
        {
            return null;
        }
    }

    /**
     * Gets the list of active breakpoint step indices parsed from the JSON string.
     *
     * @return the list of breakpoint step indices.
     */
    public List<Integer> getBreakpoints()
    {
        final List<Integer> list = new ArrayList<>();
        if (this.lastBreakpointsStr == null || this.lastBreakpointsStr.isEmpty() || "[]".equals(this.lastBreakpointsStr))
        {
            return list;
        }
        try
        {
            final JsonArray arr = JsonParser.parseString(this.lastBreakpointsStr).getAsJsonArray();
            for (int i = 0; i < arr.size(); i++)
            {
                list.add(arr.get(i).getAsInt());
            }
        }
        catch (final Exception e)
        {
            // ignore parsing errors
        }
        return list;
    }

    /**
     * Resets the active HUD action variable to enable capturing subsequent user commands.
     */
    public void resetHudAction()
    {
        try
        {
            Selenide.executeJavaScript("window.neoHudAction = null;");
        }
        catch (final Exception e)
        {
            // ignore
        }
    }

    /**
     * Saves the modified instruction set and custom variable bindings back into the original YAML test data file.
     * Automatically matches hierarchical lists or parameterized datasets so only the active dataset row is overwritten.
     *
     * @param performedInstructions the updated list of completed natural language instructions
     */
    public void saveYamlDataFileIfModified(final List<String> performedInstructions)
    {
        if (!this.canEdit || this.sourceFile == null)
        {
            LOG.info("Skip saving YAML: canEdit is false or sourceFile is null.");
            return;
        }

        try
        {
            final Yaml yaml = new Yaml();
            Object data = null;
            File file = new File(this.sourceFile);
            
            // Handle relative paths pointing outside resource directories.
            if (!file.exists() && !this.sourceFile.startsWith("src/test/resources/"))
            {
                file = new File("src/test/resources/" + (this.sourceFile.startsWith("/") ? this.sourceFile.substring(1) : this.sourceFile));
            }

            if (file.exists())
            {
                try (final InputStream is = new FileInputStream(file))
                {
                    data = yaml.load(is);
                }
            }

            final String newSteps = String.join("\n", performedInstructions) + "\n";
            Map<String, Object> targetDataset = null;

            if (data instanceof Map)
            {
                final Map<String, Object> root = (Map<String, Object>) data;
                root.put("steps", newSteps);
                
                if (root.containsKey("data"))
                {
                    targetDataset = findTargetDataset(root.get("data"), this.originalDataBindings);
                }
                else
                {
                    targetDataset = root;
                }
            }
            else if (data instanceof List)
            {
                targetDataset = findTargetDataset(data, this.originalDataBindings);
                
                final Map<String, Object> root = new LinkedHashMap<>();
                root.put("steps", newSteps);
                root.put("data", data);
                data = root;
            }
            else
            {
                final Map<String, Object> root = new LinkedHashMap<>();
                root.put("steps", newSteps);
                data = root;
            }

            if (targetDataset != null && this.dataBindings != null)
            {
                for (final Map.Entry<String, String> entry : this.dataBindings.entrySet())
                {
                    if (!entry.getKey().startsWith("neodymium.") && !entry.getKey().equals("steps"))
                    {
                        targetDataset.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            final DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            final Yaml writerYaml = new Yaml(options);
            
            final Path parentDir = file.toPath().getParent();
            if (parentDir != null && !Files.exists(parentDir))
            {
                Files.createDirectories(parentDir);
            }

            try (final FileWriter writer = new FileWriter(file))
            {
                writerYaml.dump(data, writer);
            }
            LOG.info("💾 Successfully updated YAML data file: {}", file.getAbsolutePath());
        }
        catch (final Exception e)
        {
            LOG.error("Failed to save YAML data file", e);
        }
    }

    /**
     * Resolves the matching dataset node in the YAML structure based on matching keys or test IDs.
     *
     * @param yamlData         the parsed YAML data structure
     * @param originalBindings the pristine data bindings of the active test iteration
     * @return the matched Map representation within the YAML tree, or {@code null} if none match
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findTargetDataset(final Object yamlData, final Map<String, String> originalBindings)
    {
        if (originalBindings == null)
        {
            return null;
        }
        
        if (yamlData instanceof Map)
        {
            final Map<String, Object> map = (Map<String, Object>) yamlData;
            if (map.containsKey("data"))
            {
                return findTargetDataset(map.get("data"), originalBindings);
            }
            return map;
        }
        else if (yamlData instanceof List)
        {
            final List<?> list = (List<?>) yamlData;
            
            // 1. Attempt exact match using the unique test ID parameter.
            final String testId = originalBindings.get("testId") != null ? originalBindings.get("testId") : originalBindings.get("TEST_ID");
            if (testId != null)
            {
                for (final Object item : list)
                {
                    if (item instanceof Map)
                    {
                        final Map<String, Object> map = (Map<String, Object>) item;
                        final String tId = map.get("testId") != null ? String.valueOf(map.get("testId")) : (map.get("TEST_ID") != null ? String.valueOf(map.get("TEST_ID")) : null);
                        if (testId.equals(tId))
                        {
                            return map;
                        }
                    }
                }
            }
            
            // 2. Perform property-by-property equality evaluation for non-framework properties.
            for (final Object item : list)
            {
                if (item instanceof Map)
                {
                    final Map<String, Object> map = (Map<String, Object>) item;
                    boolean match = true;
                    for (final Map.Entry<String, Object> entry : map.entrySet())
                    {
                        final String key = entry.getKey();
                        if (key.startsWith("neodymium."))
                        {
                            continue;
                        }
                        if (!originalBindings.containsKey(key))
                        {
                            match = false;
                            break;
                        }
                        final String originalVal = originalBindings.get(key);
                        final String yamlVal = String.valueOf(entry.getValue());
                        if (!yamlVal.equals(originalVal))
                        {
                            match = false;
                            break;
                        }
                    }
                    if (match)
                    {
                        return map;
                    }
                }
            }
            
            // 3. Fallback to the first item if it is the only defined dataset.
            if (list.size() == 1 && list.get(0) instanceof Map)
            {
                return (Map<String, Object>) list.get(0);
            }
        }
        return null;
    }
}
