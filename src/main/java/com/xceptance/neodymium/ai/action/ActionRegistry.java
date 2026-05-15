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
 * // AI-generated: Gemini 2.0 Flash
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
        register(new com.xceptance.neodymium.ai.action.plugins.BranchAction());
        register(new com.xceptance.neodymium.ai.action.plugins.JavaMethodAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ScrollAction());
        register(new com.xceptance.neodymium.ai.action.plugins.KeyPressAction());
        register(new com.xceptance.neodymium.ai.action.plugins.BackAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ForwardAction());
        register(new com.xceptance.neodymium.ai.action.plugins.RefreshAction());
        register(new com.xceptance.neodymium.ai.action.plugins.ClearCookiesAction());
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
