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

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Unit test for {@link SelectAction} verifying exact, value, index, and prefix/fuzzy option selection.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public class SelectActionTest extends BaseAiTest
{
    @Test
    @DisplayName("SelectAction handles exact text, value, index, and prefix/currency matching")
    public void testSelectOptionMatching() throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/SelectActionTest/test.html", server.getPort());
        Selenide.open(pageUrl);

        final SelectAction plugin = new SelectAction();

        // 1. Exact text selection
        plugin.execute(new Action("SELECT", "#size-select", "Medium", "select size", "reasoning", false));
        Assertions.assertEquals("m", $("#size-select").getValue());

        // 2. Index selection (0 = "Small", 2 = "Large")
        plugin.execute(new Action("SELECT", "#size-select", "2", "select index 2", "reasoning", false));
        Assertions.assertEquals("l", $("#size-select").getValue());

        // 3. Exact value selection
        plugin.execute(new Action("SELECT", "#country-select", "DE", "select by value DE", "reasoning", false));
        Assertions.assertEquals("DE", $("#country-select").getValue());

        // 4. Prefix / partial text selection (e.g. 'Canada (EN)' matches 'Canada (EN) (CAD $)')
        plugin.execute(new Action("SELECT", "#country-select", "Canada (EN)", "select Canada EN", "reasoning", false));
        Assertions.assertEquals("CA_EN", $("#country-select").getValue());

        // 5. Case-insensitive prefix selection (e.g. 'canada (fr)' matches 'Canada (FR) (CAD $)')
        plugin.execute(new Action("SELECT", "#country-select", "canada (fr)", "select Canada FR case-insensitive", "reasoning", false));
        Assertions.assertEquals("CA_FR", $("#country-select").getValue());

        // 6. Partial text match for United States
        plugin.execute(new Action("SELECT", "#country-select", "United States", "select US", "reasoning", false));
        Assertions.assertEquals("US", $("#country-select").getValue());
    }

    @Test
    @DisplayName("SelectAction throws exception when option cannot be found")
    public void testSelectNonExistentOptionThrows()
    {
        final String pageUrl = String.format("http://localhost:%d/SelectActionTest/test.html", server.getPort());
        Selenide.open(pageUrl);

        final SelectAction plugin = new SelectAction();
        Assertions.assertThrows(Throwable.class, () -> {
            plugin.execute(new Action("SELECT", "#country-select", "NonExistentCountry", "select invalid", "reasoning", false));
        });
    }
}
