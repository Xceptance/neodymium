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

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.codeborne.selenide.Condition;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Validates complex enterprise edge cases (Shadow DOM boundaries, element interception overlays,
 * stale elements, and multi-window environments) for the AI action runner.
 */
public final class ActionEdgeCasesTest extends BaseAiTest
{
    @Override
    @BeforeEach
    public void setupPageUrl(final TestInfo testInfo)
    {
        final String methodName = testInfo.getTestMethod().get().getName();
        String htmlFileName;
        switch (methodName)
        {
            case "testShadowDomBoundaries":
                htmlFileName = "shadowDom.html";
                break;
            case "testElementInterceptionOverlay":
                htmlFileName = "elementIntercept.html";
                break;
            case "testStaleElements":
                htmlFileName = "staleElement.html";
                break;
            case "testMultiWindow":
                htmlFileName = "multiWindow.html";
                break;
            default:
                htmlFileName = methodName + ".html";
                break;
        }
        currentTestUrl = String.format("http://localhost:%d/ActionEdgeCasesTest/%s", server.getPort(), htmlFileName);
    }
    /**
     * Verifies that the AI can resolve and interact with elements located within a Shadow DOM boundary,
     * including inputs, checkboxes, radios, and buttons.
     */
    @NeodymiumTest
    public void testShadowDomBoundaries()
    {
        open(currentTestUrl);

        // Type inside the Shadow DOM input text field
        Neodymium.ai().execute("Type 'Shadow Test Value' into the Shadow Input text field.");

        // Check the Shadow DOM checkbox element
        Neodymium.ai().execute("Check the 'Shadow Checkbox' checkbox.");

        // Check the Shadow DOM radio button element
        Neodymium.ai().execute("Check the 'Shadow Radio' radio button.");

        // Click the button inside the Shadow DOM
        Neodymium.ai().execute("Click on the 'Submit Shadow Form' button.");

        // Verify the parent DOM was updated correctly by the shadow DOM custom element
        $("#result").shouldHave(Condition.exactText("Shadow Submitted: Shadow Test Value | Checkbox: true | Radio: true"));
    }

    /**
     * Verifies that when an action is blocked by a transient loading overlay (element interception),
     * the framework automatically performs a one-shot retry once the overlay disappears, allowing the action to succeed.
     */
    @NeodymiumTest
    public void testElementInterceptionOverlay()
    {
        open(currentTestUrl);

        // This click is initially blocked by the overlay. One-shot action retry must resolve it.
        Neodymium.ai().execute("Click on the 'Submit Transaction' button.");

        // Verify transaction completed successfully after retry
        $("#result").shouldHave(Condition.exactText("Transaction Completed successfully!"));
    }

    /**
     * Verifies that if a dynamic element frequently re-renders (causing StaleElementReferenceException),
     * the framework handles it robustly via transient retry or finding fallbacks.
     */
    @NeodymiumTest
    public void testStaleElements()
    {
        open(currentTestUrl);

        // Click the dynamic re-rendered stale button
        Neodymium.ai().execute("Click the 'Click to Refresh State' button.");

        // Verify success
        $("#result").shouldHave(Condition.exactText("Stale Element Clicked Successfully!"));
    }

    /**
     * Verifies that the AI can click target=_blank links, traverse multi-window contexts,
     * and execute actions inside the newly spawned popup window.
     */
    @NeodymiumTest
    public void testMultiWindow()
    {
        open(currentTestUrl);

        // Click a target=_blank link
        Neodymium.ai().execute("Click on the 'Open Popup Window' link.");

        // Click inside the newly opened popup window
        Neodymium.ai().execute("Click the 'Confirm in Popup' button.");

        // Assert success message inside the popup
        $("#result").shouldHave(Condition.exactText("Popup Confirmed!"));
    }
}
