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
package verla;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFolder;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Runs YAML-based Aura integration tests for product search, categories refinements, and infinite scrolling.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@DataFolder("com/xceptance/neodymium/ai/VerlaStoreSearchTest")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.REPLAY,
    VerificationMode.HUD_OFFLINE_REPLAY
})
@Tag("integration")
@Tag("verla")
public final class VerlaStoreSearchTest extends BaseAiTest
{
    /**
     * Inject dynamic server HTTP port.
     */
    @BeforeEach
    public void injectDynamicPorts()
    {
        useTempPlaybookDirectory();
        final int port = server.getPort();
        Neodymium.getData().put("port", String.valueOf(port));
    }

    /**
     * Executes the search and refinements YAML steps.
     */
    @NeodymiumTest
    public void testSearchAndRefinement()
    {
        assertAiExecution();
    }
}
