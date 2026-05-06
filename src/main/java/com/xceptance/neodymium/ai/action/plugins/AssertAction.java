package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class AssertAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(AssertAction.class);

    public static final String ACTION_NAME = "ASSERT";

    @Override
    public String getActionName() { return ACTION_NAME; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "ASSERT: assert that an element contains expected text or is in an expected state (requires target and value)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        final String expected = action.getValue();
        if ("url".equalsIgnoreCase(action.getTarget()) || "currentUrl".equalsIgnoreCase(action.getTarget()) || "pageUrl".equalsIgnoreCase(action.getTarget())) {
            if (expected == null) {
                throw new ActionExecutor.ActionExecutionException("URL assertion requires a 'value' (the expected URL)");
            }
            try {
                Selenide.Wait().until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().contains(expected));
                LOG.debug("   ✅ URL Assertion passed for: '{}'", expected);
            } catch (org.openqa.selenium.TimeoutException e) {
                String actualUrl = com.codeborne.selenide.WebDriverRunner.url();
                throw new ActionExecutor.ActionExecutionException(String.format("Assertion failed: Expected URL to contain '%s' but was '%s'", expected, actualUrl), e);
            }
            return;
        }

        final SelenideElement element = executor.findElement(action);

        if (expected == null) {
            element.should(Condition.exist);
            LOG.debug("   ✅ Element exists: {}", action);
            return;
        }

        try {
            if ("visible".equals(expected)) {
                element.shouldBe(Condition.visible);
            } else {
                element.should(Condition.or("Assertion for " + expected,
                        Condition.exactText(expected),
                        Condition.partialText(expected),
                        Condition.value(expected),
                        new ActionExecutor.PartialTextContent(expected),
                        Condition.attribute("href", expected),
                        Condition.attribute("alt", expected),
                        Condition.attribute("src", expected),
                        Condition.attribute("title", expected),
                        Condition.attribute("placeholder", expected),
                        new ActionExecutor.DataAttributeMatches("data-.*", ".*" + Pattern.quote(expected) + ".*")));
            }
            LOG.debug("   ✅ Assertion passed for: '{}'", expected);
        } catch (Throwable e) {
            String actualDetails = String.format("Text: '%s', Value: '%s', Alt: '%s'", element.getText(), element.getValue(), element.getAttribute("alt"));
            throw new ActionExecutor.ActionExecutionException(String.format("Assertion failed: '%s' not found in common attributes. Found: [%s]", expected, actualDetails), e);
        }
    }
}
