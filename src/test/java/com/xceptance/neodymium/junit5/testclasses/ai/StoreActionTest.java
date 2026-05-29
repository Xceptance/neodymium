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
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// AI-generated: Gemini 2.5 Pro

package com.xceptance.neodymium.junit5.testclasses.ai;

import java.util.List;

import org.junit.jupiter.api.Assertions;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.plugins.StoreAction;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Unit/Integration test suite for {@link StoreAction} to verify literal and standard element variable storage.
 */
@Browser("Chrome_headless")
public class StoreActionTest
{
    /**
     * Verifies that literal variable storage successfully stores a direct value without finding elements.
     */
    @NeodymiumTest
    public final void testLiteralStorageSuccess()
    {
        Selenide.open("data:text/html,<html><body><div>Dummy Page</div></body></html>");

        final StoreAction storeAction = new StoreAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("STORE");
        action.setTarget(null);
        action.setValue(List.of("animal", "brown bear"));

        Assertions.assertDoesNotThrow(() ->
        {
            storeAction.execute(action, this, executor);
        });

        Assertions.assertEquals("brown bear", executor.getVariable("animal"));
    }

    /**
     * Verifies that literal variable storage successfully stores a direct numeric/price value and normalizes it if adjust is true.
     */
    @NeodymiumTest
    public final void testLiteralStorageWithAdjustment()
    {
        Selenide.open("data:text/html,<html><body><div>Dummy Page</div></body></html>");

        final StoreAction storeAction = new StoreAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("STORE");
        action.setTarget(null);
        action.setValue(List.of("price", "$ 19.99"));
        action.setAdjust(true);

        Assertions.assertDoesNotThrow(() ->
        {
            storeAction.execute(action, this, executor);
        });

        Assertions.assertEquals("19.99", executor.getVariable("price"));
    }

    /**
     * Verifies that fallback element text storage successfully captures text from the DOM.
     */
    @NeodymiumTest
    public final void testElementStorageFallbackSuccess()
    {
        Selenide.open("data:text/html,<html><body><div id='test-element'>Captured Element Text</div></body></html>");

        final StoreAction storeAction = new StoreAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("STORE");
        action.setTarget("#test-element");
        action.setValue(List.of("myVar"));

        Assertions.assertDoesNotThrow(() ->
        {
            storeAction.execute(action, this, executor);
        });

        Assertions.assertEquals("Captured Element Text", executor.getVariable("myVar"));
    }

    /**
     * Verifies that fallback element text storage successfully captures and adjusts text from the DOM.
     */
    @NeodymiumTest
    public final void testElementStorageFallbackWithAdjustment()
    {
        Selenide.open("data:text/html,<html><body><div id='test-element'>$  49.99 </div></body></html>");

        final StoreAction storeAction = new StoreAction();
        final ActionExecutor executor = new ActionExecutor(this);

        final Action action = new Action();
        action.setType("STORE");
        action.setTarget("#test-element");
        action.setValue(List.of("myPrice"));
        action.setAdjust(true);

        Assertions.assertDoesNotThrow(() ->
        {
            storeAction.execute(action, this, executor);
        });

        Assertions.assertEquals("49.99", executor.getVariable("myPrice"));
    }
}
