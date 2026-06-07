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
package com.xceptance.neodymium.ai.integration;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies all major Aura AI features through a unified YAML-based data-driven test:
 * 1. Data injection with dynamic port and SSL port replacements.
 * 2. Visual glancing baseline dHash caches.
 * 3. Optional step silent bypassing.
 * 4. Two-phase live LLM execution and instant offline playbacks.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
@DataFolder("com/xceptance/neodymium/ai/AllFeaturesYamlTest")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.OFFLINE_REPLAY,
    VerificationMode.HUD_OFFLINE_REPLAY,
    VerificationMode.HUD_LLM
})
public final class AllFeaturesYamlTest extends BaseAiTest
{
    /**
     * Injects the dynamic HTTP and HTTPS port values into Neodymium's test data context so that
     * dynamic placeholders within YAML files (e.g. ${port} and ${httpsPort}) are properly resolved.
     * 
     * @param testInfo JUnit 5 metadata
     */
    @BeforeEach
    public void injectDynamicPorts(final TestInfo testInfo)
    {
        final int port = server.getPort();
        final int httpsPort = server.getHttpsPort();
        
        Neodymium.getData().put("port", String.valueOf(port));
        Neodymium.getData().put("httpsPort", String.valueOf(httpsPort));
    }

    /**
     * Executes the comprehensive YAML steps, validating correct two-phase (live + offline replay)
     * execution across both HTTP and HTTPS server environments dynamically.
     */
    @NeodymiumTest
    public void testAllFeaturesViaYaml()
    {
        assertAiExecution();
    }
}
