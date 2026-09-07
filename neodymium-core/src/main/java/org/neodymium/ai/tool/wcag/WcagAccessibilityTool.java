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
package org.neodymium.ai.tool.wcag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.qameta.allure.Allure;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Composite tool performing WCAG accessibility audits using the Axe-core JavaScript engine.
 * Dispatches browser scripts cleanly through {@link ToolContext#invokeTool(String, Map)}
 * without depending directly on driver singletons, formats compliance violations, and attaches
 * structured artifacts to the test execution outcome.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class WcagAccessibilityTool implements AiTool
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WcagAccessibilityTool.class);

    public static final String TOOL_NAME = "wcag_accessibility_audit";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ToolDefinition definition;

    /**
     * Constructs a WcagAccessibilityTool instance.
     */
    public WcagAccessibilityTool()
    {
        this.definition = buildDefinition();
    }

    @Override
    public ToolDefinition getDefinition()
    {
        return this.definition;
    }

    @Override
    public ToolResult execute(final ToolCall call, final ToolContext context) throws Exception
    {
        final JsonNode args = call.arguments() != null ? call.arguments() : MAPPER.createObjectNode();

        final String targetSelector = args.hasNonNull("context") ? args.path("context").asText() : "document";
        final boolean failOnViolation = args.path("fail_on_violation").asBoolean(false);
        final String minImpactStr = args.path("min_impact").asText("minor").toLowerCase(Locale.ROOT);
        final int minImpactWeight = resolveImpactWeight(minImpactStr);

        final List<String> tags = new ArrayList<>();
        if (args.has("tags") && args.path("tags").isArray())
        {
            for (final JsonNode tagNode : args.path("tags"))
            {
                tags.add(tagNode.asText());
            }
        }
        if (tags.isEmpty())
        {
            tags.add("wcag2a");
            tags.add("wcag2aa");
        }

        // 1. Check if Axe-core is loaded in the browser context
        final ToolResult checkResult = context.invokeTool("browser_execute_script", Map.of("script", "return typeof window.axe !== 'undefined';"));
        final boolean axeLoaded = checkResult != null && "true".equalsIgnoreCase(checkResult.content().trim());

        if (!axeLoaded)
        {
            final String axeScript = loadClasspathResource("/js/axe.min.js", "/ai-scripts/axe.min.js");
            if (axeScript != null && !axeScript.isBlank())
            {
                LOGGER.info("Injecting Axe-core engine into active browser session");
                context.invokeTool("browser_execute_script", Map.of("script", axeScript));
            }
            else
            {
                final String errorMsg = "Axe-core engine is not loaded on the active page and could not be resolved from classpath (/js/axe.min.js or /ai-scripts/axe.min.js).";
                LOGGER.warn(errorMsg);
                return ToolResult.error(call.callId(), errorMsg);
            }
        }

        // 2. Build axe.run script invocation
        final String optionsJson = buildOptionsJson(tags);
        final String executionScript;
        if ("document".equalsIgnoreCase(targetSelector))
        {
            executionScript = String.format(
                    "return window.axe.run(document, %s).then(function(results) { return JSON.stringify(results); });",
                    optionsJson
            );
        }
        else
        {
            executionScript = String.format(
                    "return window.axe.run('%s', %s).then(function(results) { return JSON.stringify(results); });",
                    targetSelector.replace("'", "\\'"),
                    optionsJson
            );
        }

        final ToolResult scriptResult = context.invokeTool("browser_execute_script", Map.of("script", executionScript));
        final String rawResultJson = scriptResult != null ? scriptResult.content() : "{}";

        // 3. Parse violations
        final JsonNode resultsNode;
        try
        {
            resultsNode = MAPPER.readTree(rawResultJson);
        }
        catch (final Exception e)
        {
            final String parseError = "Failed to parse Axe audit JSON response: " + e.getMessage();
            LOGGER.error(parseError, e);
            return ToolResult.error(call.callId(), parseError);
        }

        final List<JsonNode> matchingViolations = new ArrayList<>();
        int criticalCount = 0;
        int seriousCount = 0;
        int moderateCount = 0;
        int minorCount = 0;

        if (resultsNode.has("violations") && resultsNode.path("violations").isArray())
        {
            for (final JsonNode violation : resultsNode.path("violations"))
            {
                final String impact = violation.path("impact").asText("minor").toLowerCase(Locale.ROOT);
                final int weight = resolveImpactWeight(impact);
                if (weight >= minImpactWeight)
                {
                    matchingViolations.add(violation);
                    switch (impact)
                    {
                        case "critical" -> criticalCount++;
                        case "serious" -> seriousCount++;
                        case "moderate" -> moderateCount++;
                        default -> minorCount++;
                    }
                }
            }
        }

        // 4. Format audit summary report
        final StringBuilder summary = new StringBuilder();
        summary.append(String.format("WCAG Accessibility Audit Result: %d violations found (Critical: %d, Serious: %d, Moderate: %d, Minor: %d)%n",
                matchingViolations.size(), criticalCount, seriousCount, moderateCount, minorCount));

        if (matchingViolations.isEmpty())
        {
            summary.append("0 violations found. The page satisfies evaluated accessibility standards.");
        }
        else
        {
            int index = 1;
            for (final JsonNode v : matchingViolations)
            {
                final String id = v.path("id").asText();
                final String impact = v.path("impact").asText("minor").toUpperCase(Locale.ROOT);
                final String description = v.path("description").asText();
                final String helpUrl = v.path("helpUrl").asText();

                summary.append(String.format("%n%d. [%s] %s: %s%n", index++, impact, id, description));
                if (!helpUrl.isBlank())
                {
                    summary.append(String.format("   Help: %s%n", helpUrl));
                }

                if (v.has("nodes") && v.path("nodes").isArray())
                {
                    summary.append("   Affected Nodes:\n");
                    for (final JsonNode node : v.path("nodes"))
                    {
                        final String html = node.path("html").asText();
                        final String target = node.path("target").isArray() && !node.path("target").isEmpty()
                                ? node.path("target").get(0).asText()
                                : "";
                        final String failure = node.path("failureSummary").asText();

                        if (!target.isBlank())
                        {
                            summary.append(String.format("     - Target: %s%n", target));
                        }
                        if (!html.isBlank())
                        {
                            summary.append(String.format("       HTML: %s%n", html));
                        }
                        if (!failure.isBlank())
                        {
                            summary.append(String.format("       Failure: %s%n", failure));
                        }
                    }
                }
            }
        }

        final String finalSummary = summary.toString().trim();

        // 5. Attach artifacts to ToolContext
        final byte[] reportBytes = rawResultJson.getBytes(StandardCharsets.UTF_8);
        final byte[] summaryBytes = finalSummary.getBytes(StandardCharsets.UTF_8);
        context.attachArtifact("wcag-audit-report.json", "application/json", reportBytes);
        context.attachArtifact("wcag-audit-summary.txt", "text/plain", summaryBytes);

        // 6. Safe Allure attachment
        try
        {
            Allure.addAttachment("WCAG Accessibility Report", "application/json", rawResultJson, "json");
            Allure.addAttachment("WCAG Accessibility Summary", "text/plain", finalSummary, "txt");
        }
        catch (final Throwable ignored)
        {
            // Allure lifecycle may be inactive in unit tests
        }

        // 7. Check fail_on_violation
        if (failOnViolation && !matchingViolations.isEmpty())
        {
            throw new AssertionError("WCAG Accessibility Audit Failed: " + finalSummary);
        }

        return ToolResult.success(call.callId(), finalSummary);
    }

    private static String buildOptionsJson(final List<String> tags)
    {
        final ObjectNode options = MAPPER.createObjectNode();
        final ObjectNode runOnly = options.putObject("runOnly");
        runOnly.put("type", "tag");
        final ArrayNode values = runOnly.putArray("values");
        for (final String tag : tags)
        {
            values.add(tag);
        }
        return options.toString();
    }

    private static int resolveImpactWeight(final String impact)
    {
        return switch (impact)
        {
            case "critical" -> 4;
            case "serious" -> 3;
            case "moderate" -> 2;
            default -> 1;
        };
    }

    private static String loadClasspathResource(final String... candidatePaths)
    {
        for (final String path : candidatePaths)
        {
            try (final InputStream in = WcagAccessibilityTool.class.getResourceAsStream(path))
            {
                if (in != null)
                {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            catch (final Exception ignored)
            {
                // Continue to next candidate
            }
        }
        return null;
    }

    private static ToolDefinition buildDefinition()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");

        props.putObject("context")
                .put("type", "string")
                .put("description", "CSS selector scoping the audit, or 'document' for the entire page (default: document)");

        final ObjectNode tagsProp = props.putObject("tags");
        tagsProp.put("type", "array");
        tagsProp.putObject("items").put("type", "string");
        tagsProp.put("description", "Axe-core WCAG tags to audit (default: ['wcag2a', 'wcag2aa'])");

        props.putObject("fail_on_violation")
                .put("type", "boolean")
                .put("description", "Whether to throw an AssertionError when violations are found (default: false)");

        final ObjectNode minImpactProp = props.putObject("min_impact");
        minImpactProp.put("type", "string");
        minImpactProp.put("description", "Minimum violation impact level: 'minor', 'moderate', 'serious', or 'critical' (default: minor)");
        final ArrayNode impactEnum = minImpactProp.putArray("enum");
        impactEnum.add("minor");
        impactEnum.add("moderate");
        impactEnum.add("serious");
        impactEnum.add("critical");

        return new ToolDefinition(
                TOOL_NAME,
                "Performs a WCAG accessibility audit on the active page via Axe-core, detecting violations and attaching report artifacts",
                schema
        );
    }
}
