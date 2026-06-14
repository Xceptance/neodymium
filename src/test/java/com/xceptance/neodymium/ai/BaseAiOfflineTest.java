/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.ai;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.testing.MockActionExecutor;
import com.xceptance.neodymium.ai.testing.MockLlmClient;
import com.xceptance.neodymium.ai.testing.MockPageAnalyzer;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Reusable base test harness class for all offline, browserless AI tests.
 * <p>
 * This class automatically handles the boilerplate configuration required to test
 * Neodymium Aura AI features offline, including backing up and restoring system properties,
 * configuring dummy credentials to satisfy validation guards, disabling PESAP analysis, 
 * and initializing mock stand-ins (LLM client, SUT page context, Selenium executor).
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public abstract class BaseAiOfflineTest
{
    private Map<String, String> originalSystemProperties;

    protected MockLlmClient llmClient;
    protected MockPageAnalyzer pageAnalyzer;
    protected MockActionExecutor actionExecutor;
    protected AiBrowser mockBrowser;

    @BeforeEach
    public final void setupHarness()
    {
        // 1. Back up system properties to prevent test contamination
        this.originalSystemProperties = new HashMap<>();
        this.storeAndClearSystemProperty("neodymium.ai.apiKey");
        this.storeAndClearSystemProperty("neodymium.ai.agent.maxRetries");

        // 2. Configure mock settings browserlessly
        MockLlmClient.configureForOffline("mock-offline-demo-key");

        // 3. Clear data contexts
        Neodymium.setAiPlaybook(null);
        Neodymium.getData().clear();

        // 4. Initialize and register the virtual test double harness
        this.llmClient = new MockLlmClient();
        this.pageAnalyzer = new MockPageAnalyzer("<html><body>Mock SUT Page</body></html>");
        this.actionExecutor = new MockActionExecutor();
        this.mockBrowser = new AiBrowser(
                Neodymium.aiConfiguration(), 
                this, 
                this.llmClient, 
                this.pageAnalyzer, 
                this.actionExecutor
        );
        Neodymium.setAiBrowser(this.mockBrowser);
    }

    @AfterEach
    public final void teardownHarness()
    {
        // 1. Unregister and release the virtual browser
        Neodymium.setAiBrowser(null);
        if (this.mockBrowser != null)
        {
            this.mockBrowser.close();
            this.mockBrowser = null;
        }

        // 2. Clean data contexts
        Neodymium.getData().clear();
        Neodymium.setAiPlaybook(null);

        // 3. Restore backed-up system properties
        System.clearProperty("neodymium.ai.apiKey");
        System.clearProperty("neodymium.ai.agent.maxRetries");

        for (final Map.Entry<String, String> entry : this.originalSystemProperties.entrySet())
        {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private void storeAndClearSystemProperty(final String key)
    {
        final String val = System.getProperty(key);
        if (val != null)
        {
            this.originalSystemProperties.put(key, val);
            System.clearProperty(key);
        }
    }
}
