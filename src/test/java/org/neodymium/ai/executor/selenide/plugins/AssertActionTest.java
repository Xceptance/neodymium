/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.executor.selenide.plugins;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.editable;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.readonly;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Unit test for {@link AssertAction} verifying support for all DOM element states.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public class AssertActionTest extends BaseAiTest
{
    @Test
    @DisplayName("AssertAction handles all DOM element state assertions cleanly")
    public void testAssertActionDomStates() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Selenide.open(pageUrl);

        final AssertAction plugin = new AssertAction();

        // 1. Visible & Hidden
        plugin.execute(new Action("ASSERT", "#visible-btn", "visible", "check visible", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#hidden-btn", "hidden", "check hidden", "reasoning", false));

        // 2. Checked & Unchecked
        plugin.execute(new Action("ASSERT", "#newsletter-opt", "checked", "check checked", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#terms-opt", "unchecked", "check unchecked", "reasoning", false));

        // 3. Disabled & Enabled
        plugin.execute(new Action("ASSERT", "#disabled-input", "disabled", "check disabled", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#enabled-input", "enabled", "check enabled", "reasoning", false));

        // 4. Selected
        plugin.execute(new Action("ASSERT", "#opt-user", "selected", "check selected", "reasoning", false));

        // 5. Readonly & Editable
        plugin.execute(new Action("ASSERT", "#readonly-input", "readonly", "check readonly", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#enabled-input", "editable", "check editable", "reasoning", false));

        // 6. Focused
        $("#username").click();
        plugin.execute(new Action("ASSERT", "#username", "focused", "check focused", "reasoning", false));

        // 7. Boolean "true" / "false" shortcuts & attribute expressions
        plugin.execute(new Action("ASSERT", "#newsletter-opt", "true", "check true on checkbox", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#terms-opt", "false", "check false on checkbox", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#newsletter-opt", "checked=true", "check checked=true", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#terms-opt", "checked=false", "check checked=false", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#disabled-input", "disabled=true", "check disabled=true", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#enabled-input", "disabled=false", "check disabled=false", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#opt-user", "selected=true", "check selected=true", "reasoning", false));
        plugin.execute(new Action("ASSERT", "#readonly-input", "readonly=true", "check readonly=true", "reasoning", false));
    }
}
