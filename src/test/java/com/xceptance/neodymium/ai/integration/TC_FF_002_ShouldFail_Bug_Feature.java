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
// AI-generated: Gemini 3.5 Flash
package com.xceptance.neodymium.ai.integration;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Test that we fail as expected and don't continue. Verifies expected failures (bug tags)
 * for both structural text checks and visual checks.
 */
@Browser("Chrome_1500x1000")
@Tag("neofeaturetest")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.OFFLINE_REPLAY
})
public class TC_FF_002_ShouldFail_Bug_Feature extends BaseAiTest
{
    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        Neodymium.getData().put("posters.storefront.url", String.format("http://localhost:%d/AuraGlanceTest/shop-posters-homepage/index.html", server.getPort()));
    }

    /**
     * Verifies that expected bugs (failures) in text checks are caught and ignored,
     * allowing the execution to complete without failure.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void assertionFailBug() throws Throwable
    {
        assertAiExecution(() ->
        {
            try
            {
                Neodymium.ai()
                        .steps("""
                                # Homepage
                                Open ${posters.storefront.url}
                                # Verify something that is not true aka a defect and we know that
                                # Sure this is not a true bug, just made up for this test.
                                Verify that the minicart shows two items (bug).
                                # This is not executed!!!
                                Verify that the top header shows a warning about a demo application.
                                """)
                        .execute();
            }
            catch (final Throwable t)
            {
                if (t instanceof RuntimeException)
                {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error)
                {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
        });
    }

    /**
     * Verifies that if an expected bug is gone (the check succeeds), an AssertionError
     * is raised to flag that the bug has been resolved.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void assertionFailBug_BugGone() throws Throwable
    {
        assertThrows(AssertionError.class, () ->
        {
            assertAiExecution(() ->
            {
                try
                {
                    Neodymium.ai()
                            .steps("""
                                    # Homepage
                                    Open ${posters.storefront.url}
                                    # Verify something that is not true aka a defect and we know that
                                    # Sure this is not a true bug, just made up for this test.
                                    Verify that the minicart shows 0 items (bug).
                                    # This is not executed!!!
                                    Verify that the top header shows a warning about a demo application.
                                    """)
                            .execute();
                }
                catch (final Throwable t)
                {
                    if (t instanceof RuntimeException)
                    {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error)
                    {
                        throw (Error) t;
                    }
                    throw new RuntimeException(t);
                }
            });
        });
    }

    /**
     * Verifies that expected bugs (failures) in visual checks are caught and ignored,
     * allowing the execution to complete without failure.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void assertionFailVisualBug() throws Throwable
    {
        assertAiExecution(() ->
        {
            try
            {
                Neodymium.ai()
                        .steps("""
                                # Homepage
                                Open ${posters.storefront.url}
                                # Verify something that is not true aka a defect and we know that
                                # Sure this is not a true bug, just made up for this test.
                                Verify that the screen is mostly black and white (bug) (visual).
                                # This is not executed!!!
                                Verify that the top header shows a warning about a demo application.
                                """)
                        .execute();
            }
            catch (final Throwable t)
            {
                if (t instanceof RuntimeException)
                {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error)
                {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
        });
    }

    /**
     * Verifies that if an expected visual bug is gone (the check succeeds), an AssertionError
     * is raised to flag that the bug has been resolved.
     *
     * @throws Throwable if execution fails
     */
    @NeodymiumTest
    public final void assertionFailVisualBug_BugGone() throws Throwable
    {
        assertThrows(AssertionError.class, () ->
        {
            assertAiExecution(() ->
            {
                try
                {
                    Neodymium.ai()
                            .steps("""
                                    # Homepage
                                    Open ${posters.storefront.url}
                                    # Verify something that is not true aka a defect and we know that
                                    # Sure this is not a true bug, just made up for this test.
                                    Verify that the screen is mostly blue and white (bug) (visual).
                                    # This is not executed!!!
                                    Verify that the top header shows a warning about a demo application.
                                    """)
                            .execute();
                }
                catch (final Throwable t)
                {
                    if (t instanceof RuntimeException)
                    {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error)
                    {
                        throw (Error) t;
                    }
                    throw new RuntimeException(t);
                }
            });
        });
    }
}
