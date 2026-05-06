package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.AuthenticationType;
import com.codeborne.selenide.BasicAuthCredentials;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class NavigateAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(NavigateAction.class);

    @Override
    public String getActionName() { return "NAVIGATE"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        String urlPatternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.url", "(?i)^(?:open|go\\s+to|navigate\\s+to|visit|[Öö]ffne|browse\\s+to)\\s+(https?:\\/\\/\\S+?)(?=[.,!?;]?(?:\\s|$))(\\.)*$");
        java.util.regex.Matcher urlMatcher = java.util.regex.Pattern.compile(urlPatternStr).matcher(instruction.strip());
        if (urlMatcher.find()) {
            final String url = urlMatcher.group(1);
            LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
            return List.of(new Action("NAVIGATE", null, url, "Navigate to " + url));
        }

        String authPatternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.urlWithBasicAuth", "(?i)^(?:open|go\\s+to|navigate\\s+to|visit|[Öö]ffne|browse\\s+to)\\s+(https?:\\/\\/\\S+?)(?=[.,!?;]?(?:\\s|$))(?:\\s+.*?\\b(?:with|using)?\\s*basic\\s+auth\\s+(?:username|user)\\s+['\"](?<username>.*?)['\"]\\s+(?:and\\s+)?(?:password|pass)\\s+['\"](?<password>.*?)['\"]).*$");
        java.util.regex.Matcher authMatcher = java.util.regex.Pattern.compile(authPatternStr).matcher(instruction.strip());
        if (authMatcher.find()) {
            final String url = authMatcher.group(1);
            final String username = authMatcher.group("username");
            final String password = authMatcher.group("password");
            if (username != null && password != null) {
                LOG.debug("▶️ [EXEC] Direct navigation to: {}", url);
                return List.of(new Action("NAVIGATE", url, List.of(username, password), "Navigate to " + url + " with basic auth (user: " + username + ", pass: " + password + ")"));
            }
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() {
        return "NAVIGATE: navigate to a specified URL (target must be empty, value must be the URL). If basic auth is needed, set target to the URL, and value to a JSON array with username and password, e.g. [\"user\", \"pass\"].";
    }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        String url = action.getValue();
        if (url == null || url.isBlank()) {
            throw new ActionExecutor.ActionExecutionException("NAVIGATE action requires a 'value' (URL)");
        }
        if (StringUtils.isNotBlank(action.getTarget()) && action.getValues().size() > 1) {
            url = action.getTarget();
            String username = action.getValues().get(0);
            String password = action.getValues().get(1);
            LOG.debug("Navigating to: {} with basic auth {}/{}", url, username, password);
            Selenide.open(url, AuthenticationType.BASIC, new BasicAuthCredentials(username, password));
        } else {
            LOG.debug("Navigating to: {}", url);
            Selenide.open(url);
        }
        Selenide.Wait().until(d -> Selenide.executeJavaScript("return document.readyState").equals("complete"));
    }
}
