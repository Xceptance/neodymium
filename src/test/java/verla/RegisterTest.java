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
 * Runs YAML-based VERLA integration tests for register.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@DataFile("verla/RegisterTest.yaml")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.REPLAY
})
@Tag("integration")
@Tag("verla")
public final class RegisterTest extends BaseAiTest
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
     * Executes the register YAML steps for United States.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "us")
    public void testRegisterUs() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for United Kingdom.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "uk")
    public void testRegisterUk() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for Canada (EN).
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "ca_en")
    public void testRegisterCaEn() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for Canada (FR).
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "ca_fr")
    public void testRegisterCaFr() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for Germany.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "de")
    public void testRegisterDe() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for Poland.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "pl")
    public void testRegisterPl() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for Sweden.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "se")
    public void testRegisterSe() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for Finland.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "fi")
    public void testRegisterFi() throws Throwable
    {
        assertAiExecution();
    }

    /**
     * Executes the register YAML steps for Japan.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    @DataSet(id = "jp")
    public void testRegisterJp() throws Throwable
    {
        assertAiExecution();
    }
}
