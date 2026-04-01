package com.xceptance.neodymium.ai.core;

import java.util.List;

import com.xceptance.neodymium.ai.action.Action;

/**
 * Helper class to format the AI agent's execution log into a nicely readable HTML document.
 * This HTML is attached to the Allure report, allowing users to inspect the full discussion
 * with the LLM (including prompts and DOM context) without downloading text files.
 */
public class AiDiscussionLogger
{
    private final StringBuilder html = new StringBuilder();

    public AiDiscussionLogger(String instructions)
    {
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<style>\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #333; }\n");
        html.append("h2 { border-bottom: 1px solid #eee; padding-bottom: 5px; }\n");
        html.append("h3 { margin-top: 20px; color: #0056b3; }\n");
        html.append("h4 { color: #555; }\n");
        html.append(".instruction { background: #f8f9fa; padding: 15px; border-left: 4px solid #0056b3; margin-bottom: 20px; }\n");
        html.append(".step { margin-bottom: 30px; border: 1px solid #ddd; padding: 15px; border-radius: 5px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n");
        html.append(".attempt { margin-left: 15px; border-left: 2px solid #ddd; padding-left: 15px; }\n");
        html.append("details { background: #fdfdfd; padding: 10px; border: 1px solid #eee; border-radius: 4px; margin-bottom: 10px; }\n");
        html.append("summary { font-weight: bold; cursor: pointer; }\n");
        html.append("pre { white-space: pre-wrap; background: #272822; color: #f8f8f2; padding: 10px; border-radius: 4px; overflow-x: auto; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 13px; }\n");
        html.append(".actions { background: #e9ecef; padding: 10px; border-radius: 4px; margin-bottom: 10px; }\n");
        html.append(".actions ul { margin: 5px 0 0 0; }\n");
        html.append(".reasoning { font-style: italic; color: #495057; margin-bottom: 10px; }\n");
        html.append(".error { color: #dc3545; font-weight: bold; padding: 10px; background: #f8d7da; border: 1px solid #f5c6cb; border-radius: 4px; margin-bottom: 10px; }\n");
        html.append(".warning { color: #856404; background: #fff3cd; padding: 10px; border: 1px solid #ffeeba; border-radius: 4px; margin-bottom: 10px; }\n");
        html.append(".success { color: #155724; background: #d4edda; padding: 10px; border: 1px solid #c3e6cb; border-radius: 4px; margin-bottom: 10px; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<h2>AI Agent Execution Log</h2>\n");
        html.append("<div class=\"instruction\"><strong>Instructions:</strong><br/>").append(escape(instructions).replace("\n", "<br/>")).append("</div>\n");
    }

    public void startStep(int current, int total, String step)
    {
        html.append("<div class=\"step\">\n");
        html.append("<h3>Step [").append(current).append("/").append(total).append("]: ").append(escape(step)).append("</h3>\n");
    }

    public void endStep()
    {
        html.append("</div>\n");
    }

    public void startAttempt(String attemptLabel)
    {
        html.append("<div class=\"attempt\">\n");
        html.append("<h4>").append(escape(attemptLabel)).append("</h4>\n");
    }

    public void endAttempt()
    {
        html.append("</div>\n");
    }

    public void logPrompt(String prompt)
    {
        html.append("<details><summary>AI Prompt (Includes DOM Context)</summary>\n");
        html.append("<pre><code>").append(escape(prompt)).append("</code></pre>\n");
        html.append("</details>\n");
    }

    public void logResponse(String response)
    {
        html.append("<details><summary>AI Response (JSON)</summary>\n");
        html.append("<pre><code>").append(escape(response)).append("</code></pre>\n");
        html.append("</details>\n");
    }

    public void logReasoning(String reasoning)
    {
        html.append("<div class=\"reasoning\"><strong>Reasoning:</strong> ").append(escape(reasoning)).append("</div>\n");
    }

    public void logError(String message)
    {
        html.append("<div class=\"error\">").append(escape(message)).append("</div>\n");
    }

    public void logWarning(String message)
    {
        html.append("<div class=\"warning\">").append(escape(message)).append("</div>\n");
    }

    public void logSuccess(String message)
    {
        html.append("<div class=\"success\">").append(escape(message)).append("</div>\n");
    }

    public void logActions(List<Action> actions)
    {
        html.append("<div class=\"actions\"><strong>Actions Executed:</strong><ul>\n");
        for (Action a : actions)
        {
            html.append("<li>").append(escape(a.getDescription())).append(" (<strong>").append(escape(a.getType().name())).append("</strong>)</li>\n");
        }
        html.append("</ul></div>\n");
    }

    public String generateHtml()
    {
        return html.toString() + "</body>\n</html>";
    }

    private String escape(String s)
    {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
