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
package com.xceptance.neodymium.ai.util;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.xceptance.neodymium.util.Neodymium;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility loader for Pre-Execution Static Analysis Phase (PESAP) custom rules files.
 * Supports loading from classpath resources, filesystem paths, and default fallback files.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class CustomRulesLoader
{
    private static final Logger LOG = LoggerFactory.getLogger(CustomRulesLoader.class);

    private CustomRulesLoader()
    {
        // Utility constructor
    }

    /**
     * Loads the custom PESAP rules content based on property value, thread-local overrides, and fallbacks.
     *
     * @param configuredFile the configured custom rules file path from property (can be null/empty)
     * @return the loaded custom rules contents, or an empty string if none are present
     */
    public static String loadCustomRules(final String configuredFile)
    {
        // 1. Thread-local programmatic override
        String fileTarget = null;
        if (Neodymium.getData() != null && Neodymium.getData().exists("neodymium.ai.pesap.custom.file"))
        {
            fileTarget = Neodymium.getData().asString("neodymium.ai.pesap.custom.file");
        }

        // 2. Explicit configuration property
        if (fileTarget == null && configuredFile != null && !configuredFile.trim().isEmpty())
        {
            fileTarget = configuredFile.trim();
        }

        if (fileTarget != null)
        {
            // Try loading from classpath first
            final String classpathContent = loadFromClasspath(fileTarget);
            if (classpathContent != null)
            {
                LOG.debug("Loaded PESAP custom rules from classpath resource: {}", fileTarget);
                return classpathContent;
            }

            // Try loading from filesystem next
            final String filesystemContent = loadFromFileSystem(fileTarget);
            if (filesystemContent != null)
            {
                LOG.debug("Loaded PESAP custom rules from filesystem path: {}", fileTarget);
                return filesystemContent;
            }

            // Configured but not found anywhere -> Strict validation error
            final String errorMsg = "Configured PESAP custom rules file not found on classpath or filesystem: " + fileTarget;
            LOG.error("❌ {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 3. Fallbacks when not explicitly configured
        // Precedence: config/pesap-custom-rules.md -> config/pesap-custom-rules.txt ->
        //             ai-prompts/pesap-custom-rules.md -> ai-prompts/pesap-custom-rules.txt
        
        // Default Filesystem Fallbacks
        final String fsMdContent = loadFromFileSystem("config/pesap-custom-rules.md");
        if (fsMdContent != null)
        {
            LOG.debug("Loaded default PESAP custom rules from filesystem fallback: config/pesap-custom-rules.md");
            return fsMdContent;
        }

        final String fsTxtContent = loadFromFileSystem("config/pesap-custom-rules.txt");
        if (fsTxtContent != null)
        {
            LOG.debug("Loaded default PESAP custom rules from filesystem fallback: config/pesap-custom-rules.txt");
            return fsTxtContent;
        }

        // Default Classpath Fallbacks
        final String cpMdContent = loadFromClasspath("ai-prompts/pesap-custom-rules.md");
        if (cpMdContent != null)
        {
            LOG.debug("Loaded default PESAP custom rules from classpath fallback: ai-prompts/pesap-custom-rules.md");
            return cpMdContent;
        }

        final String cpTxtContent = loadFromClasspath("ai-prompts/pesap-custom-rules.txt");
        if (cpTxtContent != null)
        {
            LOG.debug("Loaded default PESAP custom rules from classpath fallback: ai-prompts/pesap-custom-rules.txt");
            return cpTxtContent;
        }

        // Disabled state / No rules
        LOG.debug("No PESAP custom rules file loaded (none configured or found in default fallbacks).");
        return "";
    }

    private static String loadFromClasspath(final String resourcePath)
    {
        try (final InputStream is = CustomRulesLoader.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is != null)
            {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to read classpath resource: {}", resourcePath, e);
        }
        return null;
    }

    private static String loadFromFileSystem(final String filePath)
    {
        final File file = new File(filePath);
        if (file.exists() && file.isFile())
        {
            try
            {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to read filesystem file: {}", filePath, e);
            }
        }
        return null;
    }
}
