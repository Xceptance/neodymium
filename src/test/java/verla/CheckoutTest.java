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
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Runs YAML-based Aura integration tests for registration, login, checkout forms, guest checkouts, tracking, and purchase rules.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@DataFile("verla/CheckoutTest.yaml")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.REPLAY
})
@Tag("integration")
@Tag("verla")
public final class CheckoutTest extends BaseAiTest
{
    /**
     * Setup method to inject the dynamic server port and set temporary playbook directory.
     */
    @BeforeEach
    public void setup()
    {
        useTempPlaybookDirectory();
        Neodymium.getData().put("verla.url.host", String.format("localhost:%d", server.getPort()));
    }

    /**
     * Executes the checkout and purchase YAML steps on the bad environment.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "bad")
    public void testCheckoutBad() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the checkout and purchase YAML steps on the normal environment.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "normal")
    public void testCheckoutNormal() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the checkout and purchase YAML steps on the perfect environment.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "perfect")
    public void testCheckoutPerfect() throws Throwable
    {
        assertAiExecution();
    }
}
