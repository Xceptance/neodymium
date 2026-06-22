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
package com.xceptance.neodymium.ai.core;
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
 * Verifies YAML data-driven capabilities of Aura AI by executing tests specified in an external YAML file.
 * The test automatically runs for each dataset iteration defined in the YAML file and validates correct 
 * two-phase (live + offline replay) execution.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
@DataFolder("com/xceptance/neodymium/ai/YamlDataDrivenTest")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.OFFLINE_REPLAY,
    VerificationMode.HUD_OFFLINE_REPLAY,
    VerificationMode.HUD_LLM
})
/**
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class YamlDataDrivenTest extends BaseAiTest
{
    private String formsUrl;

    /**
     * Resolves the Forms page URL and injects it into Neodymium's test data context so that
     * dynamic placeholders within YAML files (e.g. ${formsUrl}) are properly resolved.
     * 
     * @param testInfo JUnit 5 metadata
     */
    @BeforeEach
    public void injectDynamicVariables(final TestInfo testInfo)
    {
        final int port = server.getPort();
        formsUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/forms.html", port);
        
        Neodymium.getData().put("formsUrl", formsUrl);
    }

    /**
     * Executes the YAML steps implicitly from the active dataset row, enforcing 
     * two-phase execution verification (live LLM call for caching followed by instant offline replay).
     */
    @NeodymiumTest
    public void testYamlStepsAndDataResolution()
    {
        assertAiExecution();
    }
}
