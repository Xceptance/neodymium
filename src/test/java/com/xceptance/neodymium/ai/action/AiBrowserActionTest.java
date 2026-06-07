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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.testing.AiMockResponse;

/**
 * Test cases verifying browser action invocation and sequence ordering.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class AiBrowserActionTest extends BaseAiOfflineTest
{
    /**
     * Recipe 3: Intercepting and Verifying Action Sequence Order.
     * Use MockActionExecutor to assert that correct Selenium/WebDriver interactions
     * are performed by the execution loop in the correct relative sequence, without running real browsers.
     */
    @Test
    public final void testBrowserActionSequenceVerification()
    {
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "CLICK", "tg": "#input-field"}],
                      "d": false
                    }
                    """)
                .build());
        this.llmClient.addResponse(AiMockResponse.builder()
                .responseText(
                    """
                    {
                      "s": true,
                      "r": "Success",
                      "a": [{"t": "TYPE", "tg": "#input-field", "v": "Demo"}],
                      "d": true
                    }
                    """)
                .build());

        this.mockBrowser.execute("Enter 'Demo' into input field");

        final List<Action> actionLog = this.actionExecutor.getExecutedActions();
        Assertions.assertEquals(2, actionLog.size());
        Assertions.assertTrue(actionLog.get(0).toString().contains("CLICK"));
        Assertions.assertTrue(actionLog.get(1).toString().contains("TYPE"));
        Assertions.assertTrue(actionLog.get(1).toString().contains("Demo"));
    }
}
