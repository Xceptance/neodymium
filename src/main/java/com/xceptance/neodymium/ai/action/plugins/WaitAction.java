package com.xceptance.neodymium.ai.action.plugins;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class WaitAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(WaitAction.class);

    @Override
    public String getActionName() { return "WAIT"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "WAIT: wait for a specified duration or condition (requires value in ms)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        String target = action.getTarget();
        if (target != null && !target.isBlank()) {
            long timeoutMs = 10000; // ELEMENT_TIMEOUT.toMillis()
            if (action.getValue() != null && !action.getValue().isBlank()) {
                try {
                    timeoutMs = Long.parseLong(action.getValue());
                } catch (NumberFormatException e) {
                    LOG.warn("Invalid timeout value for WAIT action: {}", action.getValue());
                }
            }
            LOG.debug("Waiting up to {} ms for element: {}", timeoutMs, target);
            executor.findElement(action).shouldBe(Condition.visible, Duration.ofMillis(timeoutMs));
        } else {
            int ms = 1000;
            if (action.getValue() != null && !action.getValue().isBlank()) {
                try {
                    ms = Integer.parseInt(action.getValue());
                } catch (NumberFormatException e) {
                    LOG.warn("Invalid sleep value for WAIT action: {}", action.getValue());
                }
            }
            LOG.debug("Sleeping for {} ms", ms);
            Selenide.sleep(ms);
        }
    }
}
