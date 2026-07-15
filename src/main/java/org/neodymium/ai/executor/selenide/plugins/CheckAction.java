/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 *
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
package org.neodymium.ai.executor.selenide.plugins;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import com.codeborne.selenide.SelenideElement;

/**
 * Concrete action plugin to check/select checkboxes and radio buttons.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class CheckAction implements BrowserActionPlugin
{
    /**
     * Constructs a CheckAction.
     */
    public CheckAction()
    {
    }

    /**
     * Checks or selects the target checkbox or radio button if not already selected.
     *
     * @param action the check action
     * @throws Exception if execution fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action != null && action.getTarget() != null && !action.getTarget().isBlank())
        {
            final SelenideElement element = SelenideElementFinder.findElement(action.getTarget());
            if (!element.isSelected())
            {
                element.click();
            }
        }
    }
}
