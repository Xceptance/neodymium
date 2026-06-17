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
package com.xceptance.neodymium.ai.action;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Registry for all available AI Actions. Instantiates plugins once on first use 
 * and caches them for future executions.
  *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
*/
public class ActionRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ActionRegistry.class);

    private static final Map<String, AiActionPlugin> plugins = new LinkedHashMap<>();
    private static boolean initialized = false;

    private ActionRegistry() {
        // static utility class
    }

    /**
     * Initializes the registry by loading core plugins and any custom plugins
     * defined in the Neodymium configuration.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        LOG.debug("Initializing AI Action Registry...");

        registerCorePlugins();

        // Load Custom Plugins from config
        AiConfiguration config = Neodymium.aiConfiguration();
        List<String> pluginClasses = config.aiPlugins();
        if (pluginClasses != null) {
            for (String className : pluginClasses) {
                if (className != null && !className.isBlank()) {
                    try {
                        Class<?> clazz = Class.forName(className.trim());
                        if (AiActionPlugin.class.isAssignableFrom(clazz)) {
                            AiActionPlugin plugin = (AiActionPlugin) clazz.getDeclaredConstructor().newInstance();
                            register(plugin);
                        } else {
                            throw new RuntimeException("Class " + className + " does not implement AiActionPlugin.");
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load AI Action plugin: " + className + ". Make sure it exists and has a public no-args constructor.", e);
                    }
                }
            }
        }

        initialized = true;
    }

    private static void registerCorePlugins()
    {
        // Register core plugins
        register(new com.xceptance.neodymium.ai.action.plugins.NavigateAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ClickAction());
        register(new com.xceptance.neodymium.ai.action.plugins.TypeAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ClearAction());
        register(new com.xceptance.neodymium.ai.action.plugins.SelectAction());
        register(new com.xceptance.neodymium.ai.action.plugins.HoverAction());
        register(new com.xceptance.neodymium.ai.action.plugins.AssertAction());
        register(new com.xceptance.neodymium.ai.action.plugins.CheckAction());
        register(new com.xceptance.neodymium.ai.action.plugins.WaitAction());
        register(new com.xceptance.neodymium.ai.action.plugins.StoreAction());
        register(new com.xceptance.neodymium.ai.action.plugins.BranchAction());
        register(new com.xceptance.neodymium.ai.action.plugins.JavaMethodAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ScrollAction());
        register(new com.xceptance.neodymium.ai.action.plugins.KeyPressAction());
        register(new com.xceptance.neodymium.ai.action.plugins.BackAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ForwardAction());
        register(new com.xceptance.neodymium.ai.action.plugins.RefreshAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ClearCookiesAction());
        register(new com.xceptance.neodymium.ai.action.plugins.SplitAction());
    }

    /**
     * Allows registering an action directly. Mostly used internally or by tests.
     */
    public static void register(AiActionPlugin plugin) {
        if (plugin == null || plugin.getActionName() == null) {
            return;
        }
        plugins.put(plugin.getActionName().toUpperCase(), plugin);
        LOG.debug("Registered AI Action Plugin: {}", plugin.getActionName());
    }

    /**
     * Gets a plugin by its action name (case-insensitive).
     */
    public static AiActionPlugin getPlugin(String actionName) {
        if (!initialized) {
            init();
        }
        if (actionName == null) {
            return null;
        }
        return plugins.get(actionName.toUpperCase());
    }

    /**
     * Returns all registered plugins.
     */
    public static Collection<AiActionPlugin> getAllPlugins() {
        if (!initialized) {
            init();
        }
        return plugins.values();
    }
}
