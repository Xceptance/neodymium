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
package com.xceptance.neodymium.aura.interactive.unit;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;

/**
 * Selenide integration test verifying data bindings filtering in the Interactive View:
 * - Test data overview table filters out unused global properties.
 * - Step edit card renders Local badges for YAML dataset variables, Used badges for step instruction variables,
 *   and collapses unused properties under the "+ Show More" button.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
public final class InteractiveViewDataBindingsFilterTest extends BaseInteractiveViewTest
{
    private void jsClick(final SelenideElement element)
    {
        for (int i = 0; i < 3; i++)
        {
            try
            {
                com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));", element);
                return;
            }
            catch (final org.openqa.selenium.StaleElementReferenceException e)
            {
                if (i == 2)
                {
                    throw e;
                }
                com.codeborne.selenide.Selenide.sleep(200);
            }
        }
    }

    @Test
    public final void testDataOverviewAndInlineEditorFiltering() throws Exception
    {
        // Load base test resources
        final String yamlContent;
        final String jsonContent;

        try (final InputStream yamlIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.yaml");
             final InputStream jsonIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.json"))
        {
            if (yamlIs == null || jsonIs == null)
            {
                throw new IllegalStateException("Required test resources not found on classpath.");
            }
            yamlContent = new String(yamlIs.readAllBytes(), StandardCharsets.UTF_8);
            jsonContent = new String(jsonIs.readAllBytes(), StandardCharsets.UTF_8);
        }

        Configuration.clickViaJs = true;

        // Modify YAML data section so local bindings only include testId and streetAddress,
        // and step instruction references usedGlobalProp.
        final String modifiedYaml = yamlContent
            .replace("email: \"john.doe@example.com\"", "# email omitted")
            .replace("password: \"securePassword123\"", "# password omitted")
            .replace("Enter '${streetAddress}' into the Street input field.", "Enter '${streetAddress}' and '${usedGlobalProp}' into the Street input field.");

        // Inject global dataBindings into mock JSON
        final String enrichedJson = jsonContent.replace("run_2026-06-29_10-30-00", this.engine.getRunId())
            .replace("\"dataBindings\": {", "\"dataBindings\": {\n    \"usedGlobalProp\": \"usedVal\",\n    \"unusedGlobalProp1\": \"unusedVal1\",\n    \"unusedGlobalProp2\": \"unusedVal2\",");

        final Thread simThread = new Thread(() -> 
        {
            InteractiveConsoleServer.runSimulation(this.engine, modifiedYaml, enrichedJson);
        }, "DataBindingsFilterTest-Simulation");
        simThread.setDaemon(true);
        simThread.start();

        openConsoleUrl();

        // 1. Verify Test Details drawer (#topDetailsPanel) test data overview filtering
        jsClick($("#btnToggleDetails"));
        $("#topTestDataBody").should(Condition.exist);

        // Local & Used properties MUST be visible in #topTestDataBody
        $("#topTestDataBody").shouldHave(Condition.text("streetAddress"));
        $("#topTestDataBody").shouldHave(Condition.text("testId"));
        $("#topTestDataBody").shouldHave(Condition.text("usedGlobalProp"));

        // Unused properties MUST NOT be visible in #topTestDataBody
        $("#topTestDataBody").shouldNotHave(Condition.text("unusedGlobalProp1"));
        $("#topTestDataBody").shouldNotHave(Condition.text("unusedGlobalProp2"));

        // 2. Verify Step 8 (3rd step in steps block) Inline Editor badges and Show More toggle
        final SelenideElement step2 = $(".step-card[data-step-idx='8']");
        step2.should(Condition.exist);

        jsClick(step2.$(".step-edit-btn"));
        step2.$(".inline-edit-textarea").should(Condition.exist);

        // Local properties MUST have Local badge
        step2.$$(".editable-binding-key").findBy(Condition.text("streetAddress")).$(".badge-tag").shouldHave(Condition.text("Local"));
        step2.$$(".editable-binding-key").findBy(Condition.text("testId")).$(".badge-tag").shouldHave(Condition.text("Local"));

        // Used properties MUST have Used badge
        step2.$$(".editable-binding-key").findBy(Condition.text("usedGlobalProp")).$(".badge-tag").shouldHave(Condition.text("Used"));

        // Unused properties MUST NOT be visible in primary table
        step2.$$(".editable-binding-key").findBy(Condition.text("unusedGlobalProp1")).shouldNot(Condition.exist);

        // Click "+ Show More" button
        step2.$(".btn-more-props").should(Condition.exist);
        jsClick(step2.$(".btn-more-props"));

        // Collapsible container MUST expand and show unused properties
        step2.$(".more-props-container").shouldBe(Condition.visible);
        step2.$(".more-props-container").shouldHave(Condition.text("unusedGlobalProp1"));
        step2.$(".more-props-container").shouldHave(Condition.text("unusedGlobalProp2"));

        // Click unused property in expanded section to verify variable insertion
        jsClick(step2.$$(".more-props-container .editable-binding-key").findBy(Condition.text("unusedGlobalProp1")));
        step2.$(".inline-edit-textarea").shouldHave(Condition.value("${unusedGlobalProp1}Enter '${streetAddress}' and '${usedGlobalProp}' into the Street input field."));

        // Cancel editing
        jsClick(step2.$(".inline-edit-actions .btn"));
    }
}
