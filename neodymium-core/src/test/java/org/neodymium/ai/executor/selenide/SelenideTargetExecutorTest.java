/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.executor.selenide;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Unit tests for {@link SelenideTargetExecutor} action interpolation and variable resolution.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class SelenideTargetExecutorTest
{
    @Test
    public void testResolveVariablesResolvesUnbracketedInStandardAction()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("password", "Secret123!", true);
        sessionData.putDynamic("user.email", "tester@neodymium.org", false);

        final ExecutionContext context = new ExecutionContext(sessionData);
        final SelenideTargetExecutor executor = new SelenideTargetExecutor();
        executor.setExecutionContext(context);

        final Action action = new Action("TYPE", "#password", "Type secret").withValue("$password");
        final Action resolved = executor.resolveVariables(action);

        Assertions.assertNotNull(resolved);
        Assertions.assertEquals("Secret123!", resolved.getValue());
        Assertions.assertEquals("#password", resolved.getTarget());

        final Action navAction = new Action("NAVIGATE", "https://example.com/login?user=$user.email", "Navigate to login");
        final Action resolvedNav = executor.resolveVariables(navAction);
        Assertions.assertEquals("https://example.com/login?user=tester@neodymium.org", resolvedNav.getTarget());
    }

    @Test
    public void testResolveVariablesPreservesJavaScriptVariablesInScriptAction()
    {
        final SessionData sessionData = new SessionData();
        sessionData.putDynamic("btn", "button#submit", false);
        sessionData.putDynamic("token", "secretToken", true);

        final ExecutionContext context = new ExecutionContext(sessionData);
        final SelenideTargetExecutor executor = new SelenideTargetExecutor();
        executor.setExecutionContext(context);

        final String jsScript = "var $btn = document.querySelector('button'); return $btn.textContent + ' ' + '${token}';";
        final Action scriptAction = new Action("EXECUTE_SCRIPT", jsScript, "Execute script");
        final Action resolved = executor.resolveVariables(scriptAction);

        Assertions.assertNotNull(resolved);
        Assertions.assertEquals("var $btn = document.querySelector('button'); return $btn.textContent + ' ' + 'secretToken';", resolved.getTarget());
    }
}
