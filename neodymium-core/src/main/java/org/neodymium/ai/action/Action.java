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
package org.neodymium.ai.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.tool.ToolCall;

/**
 * Represents a single executable action parsed from LLM response or recording.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private List<Action> condition;
    private List<Action> then;
    @JsonProperty("else")
    private List<Action> elseActions;
    @JsonProperty("hasElse")
    private Boolean hasElse;
    private boolean adjust = false;
    /**
     * The domain action type (e.g. "CLICK", "TYPE", "NAVIGATE").
     */
    private final String type;

    /**
     * The selector or target locator string.
     */
    private final String target;

    /**
     * List of values/arguments for the action.
     */
    private final List<String> value;

    /**
     * Human-readable description of what this action does.
     */
    private final String description;

    /**
     * LLM reasoning for why this action was chosen.
     */
    private final String reasoning;

    @JsonProperty("isRegex")
    private boolean isRegex = false;

    private String stepInstruction;
    private int stepLine = -1;
    private String stepFile;
    private String stepScreenshotHash;
    private Long durationMs;
    private Long delayMs;

    /**
     * Self-critique generated during recording for playbook quality tracking.
     */
    @JsonProperty("selfCritique")
    private String selfCritique;

    /**
     * Ranked alternative candidate locators provided by LLM extraction or recording.
     */
    @JsonProperty("candidateLocators")
    private List<LocatorCandidate> candidateLocators = new ArrayList<>();

    /**
     * The DOM Feature Vector snapshot of the target interactive element.
     */
    @JsonProperty("domFeatureVector")
    private DomFeatureVector domFeatureVector;

    /**
     * Associated structured tool call for unified tooling execution and serialization.
     */
    @JsonProperty("toolCall")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToolCall toolCall;

    /**
     * Dynamic parameter binding bindings for extensible runtime properties.
     */
    private final transient Map<String, Object> parameters = new HashMap<>();

    /**
     * Constructs a default empty action.
     */
    public Action()
    {
        this.type = "";
        this.target = "";
        this.value = new ArrayList<>();
        this.description = "";
        this.reasoning = "";
    }

    /**
     * JsonCreator constructor for Jackson deserialization of final fields.
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Action(
            @JsonProperty(value = "type", required = false) @JsonAlias({"type", "action"}) final String type,
            @JsonProperty(value = "target", required = false) @JsonAlias({"locator", "target", "selector", "url", "script"}) final String target,
            @JsonProperty(value = "value", required = false) @JsonAlias({"values", "value", "text", "expectedText", "key"}) final Object value,
            @JsonProperty(value = "description", required = false) final String description,
            @JsonProperty(value = "reasoning", required = false) @JsonAlias({"reasoning", "thought"}) final String reasoning,
            @JsonProperty(value = "isRegex", required = false) @JsonAlias({"isRegex", "regex"}) final Boolean isRegex)
    {
        this.type = type != null ? type : "";
        this.target = target != null ? target : "";
        this.value = new ArrayList<>();
        if (value instanceof List<?> list)
        {
            for (final Object item : list)
            {
                if (item != null)
                {
                    this.value.add(item.toString());
                }
            }
        }
        else if (value instanceof String str && !str.isEmpty())
        {
            this.value.add(str);
        }
        else if (value != null)
        {
            final String str = value.toString().replaceAll("^\"|\"$", "");
            if (!str.isEmpty())
            {
                this.value.add(str);
            }
        }
        this.description = description != null ? description : "";
        this.reasoning = reasoning != null ? reasoning : "";
        this.isRegex = Boolean.TRUE.equals(isRegex);
    }

    /**
     * Constructs an action with basic type, target, and description.
     *
     * @param type the action type (e.g. "CLICK")
     * @param target the target selector or URL
     * @param description the human-readable description
     */
    public Action(final String type, final String target, final String description)
    {
        this.type = type;
        this.target = target;
        this.value = new ArrayList<>();
        this.description = description;
        this.reasoning = "";
    }

    /**
     * Constructs a fully detailed action.
     *
     * @param type the action type (e.g. "TYPE")
     * @param target the target selector or URL
     * @param value the action values list
     * @param description the human-readable description
     * @param reasoning the reasoning of the action
     */
    public Action(final String type, final String target, final List<String> value, final String description, final String reasoning)
    {
        this(type, target, value, description, reasoning, false);
    }

    /**
     * Constructs a fully detailed action including regex flag.
     *
     * @param type the action type (e.g. "TYPE")
     * @param target the target selector or URL
     * @param value the action values list
     * @param description the human-readable description
     * @param reasoning the reasoning of the action
     * @param isRegex whether target is a regex pattern
     */
    public Action(final String type, final String target, final List<String> value, final String description, final String reasoning, final boolean isRegex)
    {
        this.type = type;
        this.target = target;
        this.value = value != null ? new ArrayList<>(value) : new ArrayList<>();
        this.description = description;
        this.reasoning = reasoning;
        this.isRegex = isRegex;
    }

    /**
     * Returns the action type.
     *
     * @return the action type string
     */
    public final String getType()
    {
        return this.type;
    }

    /**
     * Returns the target selector or URL.
     *
     * @return the target selector or URL string
     */
    public final String getTarget()
    {
        return this.target;
    }

    /**
     * Creates a new Action copy with an updated target selector.
     *
     * @param newTarget the new target selector
     * @return a new Action instance with the updated target
     */
    public Action withTarget(final String newTarget)
    {
        final Action copy = new Action(this.type, newTarget, this.value, this.description, this.reasoning, this.isRegex);
        copy.condition = this.condition;
        copy.then = this.then;
        copy.elseActions = this.elseActions;
        copy.hasElse = this.hasElse;
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = this.domFeatureVector;
        copy.toolCall = this.toolCall;
        copy.durationMs = this.durationMs;
        copy.delayMs = this.delayMs;
        copy.parameters.putAll(this.parameters);
        return copy;
    }

    @JsonProperty("isRegex")
    public final boolean isRegex()
    {
        return this.isRegex;
    }

    @JsonProperty("isRegex")
    public final void setIsRegex(final boolean isRegex)
    {
        this.isRegex = isRegex;
    }

    public Action withIsRegex(final boolean isRegex)
    {
        final Action copy = new Action(this.type, this.target, this.value, this.description, this.reasoning, isRegex);
        copy.condition = this.condition;
        copy.then = this.then;
        copy.elseActions = this.elseActions;
        copy.hasElse = this.hasElse;
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = this.domFeatureVector;
        copy.toolCall = this.toolCall;
        copy.durationMs = this.durationMs;
        copy.delayMs = this.delayMs;
        copy.parameters.putAll(this.parameters);
        return copy;
    }

    /**
     * Creates a new Action copy with an updated target value.
     *
     * @param newValue the new action value string
     * @return a new Action instance with the updated value
     */
    public Action withValue(final String newValue)
    {
        final List<String> newValues = newValue != null && !newValue.isEmpty() ? List.of(newValue) : List.of();
        final Action copy = new Action(this.type, this.target, newValues, this.description, this.reasoning, this.isRegex);
        copy.condition = this.condition;
        copy.then = this.then;
        copy.elseActions = this.elseActions;
        copy.hasElse = this.hasElse;
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = this.domFeatureVector;
        copy.toolCall = this.toolCall;
        copy.durationMs = this.durationMs;
        copy.delayMs = this.delayMs;
        copy.parameters.putAll(this.parameters);
        return copy;
    }

    /**
     * Creates a new Action copy with an updated reasoning string.
     *
     * @param newReasoning the new reasoning string
     * @return a new Action instance with the updated reasoning
     */
    public Action withReasoning(final String newReasoning)
    {
        final Action copy = new Action(this.type, this.target, this.value, this.description, newReasoning, this.isRegex);
        copy.condition = this.condition;
        copy.then = this.then;
        copy.elseActions = this.elseActions;
        copy.hasElse = this.hasElse;
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = this.domFeatureVector;
        copy.toolCall = this.toolCall;
        copy.durationMs = this.durationMs;
        copy.delayMs = this.delayMs;
        copy.parameters.putAll(this.parameters);
        return copy;
    }

    /**
     * Creates a new Action copy with an updated DOM Feature Vector.
     *
     * @param featureVector the new DOM feature vector
     * @return a new Action instance with the updated feature vector
     */
    public Action withDomFeatureVector(final DomFeatureVector featureVector)
    {
        final Action copy = new Action(this.type, this.target, this.value, this.description, this.reasoning, this.isRegex);
        copy.condition = this.condition;
        copy.then = this.then;
        copy.elseActions = this.elseActions;
        copy.hasElse = this.hasElse;
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = featureVector;
        copy.toolCall = this.toolCall;
        copy.durationMs = this.durationMs;
        copy.delayMs = this.delayMs;
        copy.parameters.putAll(this.parameters);
        return copy;
    }

    /**
     * Returns the DOM Feature Vector snapshot of the target element.
     *
     * @return the DOM feature vector, or {@code null} if not set
     */
    public final DomFeatureVector getDomFeatureVector()
    {
        return this.domFeatureVector;
    }

    /**
     * Sets the DOM Feature Vector snapshot of the target element.
     *
     * @param domFeatureVector the DOM feature vector to set
     */
    public final void setDomFeatureVector(final DomFeatureVector domFeatureVector)
    {
        this.domFeatureVector = domFeatureVector;
    }

    /**
     * Returns the self-judging critique string.
     *
     * @return self-critique reasoning or empty string
     */
    public final String getSelfCritique()
    {
        return this.selfCritique != null ? this.selfCritique : "";
    }

    /**
     * Sets the self-judging critique string.
     *
     * @param selfCritique self-critique reasoning string
     */
    public final void setSelfCritique(final String selfCritique)
    {
        this.selfCritique = selfCritique != null ? selfCritique : "";
    }

    /**
     * Returns the list of alternative candidate locators.
     *
     * @return candidate locators list
     */
    public final List<LocatorCandidate> getCandidateLocators()
    {
        return this.candidateLocators != null ? this.candidateLocators : new ArrayList<>();
    }

    /**
     * Sets the list of alternative candidate locators.
     *
     * @param candidateLocators candidate locators to set
     */
    public final void setCandidateLocators(final List<LocatorCandidate> candidateLocators)
    {
        this.candidateLocators = candidateLocators != null ? new ArrayList<>(candidateLocators) : new ArrayList<>();
    }

    /**
     * Resolves the best candidate locator available. Returns the locator with the highest confidence
     * score, falling back to the primary target selector if no candidates exist.
     *
     * @return the resolved best candidate locator string
     */
    public final String getBestCandidateLocator()
    {
        if (this.candidateLocators == null || this.candidateLocators.isEmpty())
        {
            return this.target;
        }

        LocatorCandidate best = null;
        for (final LocatorCandidate candidate : this.candidateLocators)
        {
            if (candidate != null && candidate.getLocator() != null && !candidate.getLocator().isEmpty())
            {
                if (best == null || candidate.getScore() > best.getScore())
                {
                    best = candidate;
                }
            }
        }
        return best != null ? best.getLocator() : this.target;
    }

    /**
     * Resolves all unique candidate locator strings ordered by confidence score descending,
     * prepended with the primary target locator.
     *
     * @return ordered list of unique locator strings
     */
    public final List<String> getAllCandidateLocators()
    {
        final java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (this.target != null && !this.target.isBlank())
        {
            set.add(this.target.trim());
        }
        if (this.candidateLocators != null)
        {
            final List<LocatorCandidate> sorted = new ArrayList<>(this.candidateLocators);
            sorted.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            for (final LocatorCandidate candidate : sorted)
            {
                if (candidate != null && candidate.getLocator() != null && !candidate.getLocator().isBlank())
                {
                    set.add(candidate.getLocator().trim());
                }
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * Returns the primary value of this action, or null if no values exist.
     *
     * @return the first value in the values list, or null
     */
    @JsonProperty("value")
    public final String getValue()
    {
        return (this.value != null && !this.value.isEmpty()) ? this.value.get(0) : null;
    }

    /**
     * Returns the complete values list of this action.
     *
     * @return the values list
     */
    @JsonProperty("values")
    public final List<String> getValues()
    {
        return this.value != null ? new ArrayList<>(this.value) : new ArrayList<>();
    }

    /**
     * Returns the human-readable description of this action.
     *
     * @return the description string
     */
    public final String getDescription()
    {
        return this.description;
    }

    /**
     * Returns the reasoning behind this action.
     *
     * @return the reasoning string
     */
    public final String getReasoning()
    {
        return this.reasoning;
    }

    /**
     * Returns the dynamic extensibility parameters map.
     *
     * @return the parameters map
     */
    public final Map<String, Object> getParameters()
    {
        return this.parameters;
    }

    public final String getStepInstruction()
    {
        return this.stepInstruction;
    }

    public final void setStepInstruction(final String stepInstruction)
    {
        this.stepInstruction = stepInstruction;
    }

    public final int getStepLine()
    {
        return this.stepLine;
    }

    public final void setStepLine(final int stepLine)
    {
        this.stepLine = stepLine;
    }

    public final String getStepFile()
    {
        return this.stepFile;
    }

    public final void setStepFile(final String stepFile)
    {
        this.stepFile = stepFile;
    }

    public final String getStepScreenshotHash()
    {
        return this.stepScreenshotHash;
    }

    public final void setStepScreenshotHash(final String stepScreenshotHash)
    {
        this.stepScreenshotHash = stepScreenshotHash;
    }

    public final List<Action> getCondition()
    {
        return this.condition;
    }

    public final void setCondition(final List<Action> condition)
    {
        this.condition = condition;
    }

    public final List<Action> getThen()
    {
        return this.then;
    }

    public final void setThen(final List<Action> then)
    {
        this.then = then;
    }

    public final List<Action> getElseActions()
    {
        return this.elseActions;
    }

    public final void setElseActions(final List<Action> elseActions)
    {
        this.elseActions = elseActions;
    }

    @JsonProperty("hasElse")
    public final Boolean getHasElse()
    {
        return this.hasElse;
    }

    @JsonProperty("hasElse")
    public final void setHasElse(final Boolean hasElse)
    {
        this.hasElse = hasElse;
    }

    public final boolean hasElse()
    {
        if (this.hasElse != null)
        {
            return this.hasElse;
        }
        return this.elseActions != null && !this.elseActions.isEmpty();
    }

    public final boolean getAdjust()
    {
        return this.adjust;
    }

    public final void setAdjust(final boolean adjust)
    {
        this.adjust = adjust;
    }

    /**
     * Gets the recorded execution duration of this action in milliseconds.
     *
     * @return the execution duration in milliseconds, or {@code null} if not set
     */
    public final Long getDurationMs()
    {
        return this.durationMs;
    }

    /**
     * Sets the recorded execution duration of this action in milliseconds.
     *
     * @param durationMs the execution duration in milliseconds
     */
    public final void setDurationMs(final Long durationMs)
    {
        this.durationMs = durationMs;
    }

    /**
     * Gets the recorded delay in milliseconds before this action was executed.
     *
     * @return the pre-action delay in milliseconds, or {@code null} if not set
     */
    public final Long getDelayMs()
    {
        return this.delayMs;
    }

    /**
     * Sets the recorded delay in milliseconds before this action was executed.
     *
     * @param delayMs the pre-action delay in milliseconds
     */
    public final void setDelayMs(final Long delayMs)
    {
        this.delayMs = delayMs;
    }

    /**
     * Gets the associated tool call, or null if none.
     *
     * @return tool call or null
     */
    public ToolCall getToolCall()
    {
        return this.toolCall;
    }

    /**
     * Sets the associated tool call.
     *
     * @param toolCall the tool call
     */
    public void setToolCall(final ToolCall toolCall)
    {
        this.toolCall = toolCall;
    }

    /**
     * Converts this action into a typed {@link ToolCall}, either returning its stored
     * tool call or synthesizing one from its legacy type, target, and value properties.
     *
     * @return equivalent ToolCall
     */
    public ToolCall toToolCall()
    {
        if (this.toolCall != null)
        {
            return this.toolCall;
        }

        final ObjectNode args = MAPPER.createObjectNode();
        final String actionType = this.type != null ? this.type.toUpperCase(Locale.ROOT) : "";
        final String toolName = switch (actionType)
        {
            case "CLICK" -> "click";
            case "FILL", "TYPE" -> "fill";
            case "NAVIGATE", "OPEN" -> "navigate";
            case "SELECT" -> "select";
            case "HOVER" -> "hover";
            case "ASSERT_URL" -> "assert_url";
            case "ASSERT_TITLE" -> "assert_title";
            case "ASSERT_TEXT" -> "assert_text";
            case "ASSERT_ATTRIBUTE" -> "assert_attribute";
            case "ASSERT_VISIBLE", "ASSERT_HIDDEN", "ASSERT_ENABLED", "ASSERT_DISABLED",
                 "ASSERT_EDITABLE", "ASSERT_READONLY", "ASSERT_CHECKED", "ASSERT_UNCHECKED",
                 "ASSERT_SELECTED", "ASSERT_UNSELECTED", "ASSERT_FOCUSED", "ASSERT_EXISTS",
                 "ASSERT_ABSENT" -> "assert_element_state";
            case "ASSERT" -> {
                if ("url".equalsIgnoreCase(this.target) || "currentUrl".equalsIgnoreCase(this.target) || "pageUrl".equalsIgnoreCase(this.target))
                {
                    yield "assert_url";
                }
                if ("title".equalsIgnoreCase(this.target) || "pageTitle".equalsIgnoreCase(this.target))
                {
                    yield "assert_title";
                }
                yield "assert_text";
            }
            case "ASSERT_COUNT" -> "assert_count";
            case "EXECUTE_SCRIPT", "SCRIPT" -> "execute_script";
            case "SCROLL" -> "scroll";
            default -> {
                if (this.type != null && this.type.startsWith("browser_"))
                {
                    yield this.type.substring("browser_".length());
                }
                yield this.type != null && !this.type.isBlank() ? this.type.toLowerCase(Locale.ROOT) : "click";
            }
        };

        if ("navigate".equals(toolName) || "browser_navigate".equals(toolName))
        {
            args.put("url", this.target != null ? this.target : "");
        }
        else if ("assert_url".equals(toolName) || "browser_assert_url".equals(toolName))
        {
            final String urlVal = (this.value != null && !this.value.isEmpty()) ? this.value.get(0) : this.target;
            args.put("expectedUrl", urlVal != null ? urlVal : "");
        }
        else if ("assert_title".equals(toolName) || "browser_assert_title".equals(toolName))
        {
            final String titleVal = (this.value != null && !this.value.isEmpty()) ? this.value.get(0) : this.target;
            args.put("expectedTitle", titleVal != null ? titleVal : "");
        }
        else if ("assert_text".equals(toolName) || "browser_assert_text".equals(toolName))
        {
            final String txt = (this.value != null && !this.value.isEmpty()) ? this.value.get(0) : this.target;
            args.put("text", txt != null ? txt : "");
            if (this.target != null && !this.target.isBlank() && !"url".equalsIgnoreCase(this.target) && !"title".equalsIgnoreCase(this.target))
            {
                args.put("selector", this.target);
            }
        }
        else if ("assert_count".equals(toolName) || "browser_assert_count".equals(toolName))
        {
            args.put("selector", this.target != null ? this.target : "");
            if (this.value != null && !this.value.isEmpty())
            {
                final String v = this.value.get(0).trim();
                if (v.startsWith(">="))
                {
                    try
                    {
                        final int parsedMin = Integer.parseInt(v.substring(2).trim());
                        args.put("count", parsedMin);
                        args.put("operator", "MIN");
                        args.put("minCount", parsedMin);
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
                else if (v.startsWith("<="))
                {
                    try
                    {
                        final int parsedMax = Integer.parseInt(v.substring(2).trim());
                        args.put("count", parsedMax);
                        args.put("operator", "MAX");
                        args.put("maxCount", parsedMax);
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
                else
                {
                    try
                    {
                        final int parsedExact = Integer.parseInt(v);
                        args.put("count", parsedExact);
                        args.put("operator", "EXACT");
                        args.put("expectedCount", parsedExact);
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
            }
        }
        else if ("assert_element_state".equals(toolName) || "browser_assert_element_state".equals(toolName))
        {
            args.put("selector", this.target != null ? this.target : "");
            final String state = actionType.startsWith("ASSERT_")
                    ? actionType.substring("ASSERT_".length()).toLowerCase(Locale.ROOT)
                    : ((this.value != null && !this.value.isEmpty()) ? this.value.get(0).toLowerCase(Locale.ROOT) : "visible");
            args.put("state", state);
        }
        else if ("assert_attribute".equals(toolName) || "browser_assert_attribute".equals(toolName))
        {
            args.put("selector", this.target != null ? this.target : "");
            if (this.value != null && !this.value.isEmpty())
            {
                final String rawVal = this.value.get(0);
                final int eqIdx = rawVal.indexOf('=');
                if (eqIdx > 0)
                {
                    args.put("attribute", rawVal.substring(0, eqIdx).trim());
                    String expectedVal = rawVal.substring(eqIdx + 1).trim();
                    if ((expectedVal.startsWith("\"") && expectedVal.endsWith("\"")) || (expectedVal.startsWith("'") && expectedVal.endsWith("'")))
                    {
                        expectedVal = expectedVal.substring(1, expectedVal.length() - 1);
                    }
                    args.put("expectedValue", expectedVal);
                }
                else
                {
                    args.put("attribute", rawVal.trim());
                }
            }
        }
        else if ("execute_script".equals(toolName) || "browser_execute_script".equals(toolName))
        {
            args.put("script", this.target != null ? this.target : "");
        }
        else
        {
            args.put("target", this.target != null ? this.target : "");
            final ClickAction.CoordinateTarget coord = ClickAction.parseCoordinateTarget(this.target);
            if (coord != null)
            {
                args.put("x", coord.x());
                args.put("y", coord.y());
                if (coord.anchorSelector() != null && !coord.anchorSelector().isBlank())
                {
                    args.put("selector", coord.anchorSelector());
                }
            }
            else if (this.target != null && !this.target.isBlank())
            {
                args.put("selector", this.target);
            }

            if (this.value != null && !this.value.isEmpty())
            {
                args.put("text", this.value.get(0));
                args.put("value", this.value.get(0));
            }
        }

        if (this.domFeatureVector != null)
        {
            args.set("domFeatureVector", MAPPER.valueToTree(this.domFeatureVector));
        }

        return new ToolCall(UUID.randomUUID().toString(), toolName, args);
    }

    /**
     * Factory method creating an Action from a {@link ToolCall}.
     *
     * @param call tool call
     * @return equivalent Action instance
     */
    public static Action fromToolCall(final ToolCall call)
    {
        if (call == null)
        {
            return null;
        }

        final String rawName = call.toolName() != null ? call.toolName() : "";
        final String name = rawName.startsWith("browser_") ? rawName.substring("browser_".length()) : rawName;
        final JsonNode args = call.arguments();
        final String type = switch (name)
        {
            case "navigate" -> "NAVIGATE";
            case "click" -> "CLICK";
            case "fill", "type" -> "TYPE";
            case "hover" -> "HOVER";
            case "scroll" -> "SCROLL";
            case "select" -> "SELECT";
            case "clear" -> "CLEAR";
            case "clear_cookies" -> "CLEAR_COOKIES";
            case "back" -> "BACK";
            case "forward" -> "FORWARD";
            case "refresh" -> "REFRESH";
            case "wait" -> "WAIT";
            case "assert_text" -> "ASSERT_TEXT";
            case "assert_count" -> "ASSERT_COUNT";
            case "assert_url" -> "ASSERT_URL";
            case "assert_title" -> "ASSERT_TITLE";
            case "assert_element_state", "assert_state" -> {
                final String rawState = args != null && args.hasNonNull("state")
                        ? args.path("state").asText().trim().toUpperCase(Locale.ROOT)
                        : "";
                yield switch (rawState)
                {
                    case "VISIBLE" -> "ASSERT_VISIBLE";
                    case "HIDDEN" -> "ASSERT_HIDDEN";
                    case "ENABLED" -> "ASSERT_ENABLED";
                    case "DISABLED" -> "ASSERT_DISABLED";
                    case "EDITABLE" -> "ASSERT_EDITABLE";
                    case "READONLY", "READ_ONLY", "READ-ONLY" -> "ASSERT_READONLY";
                    case "CHECKED" -> "ASSERT_CHECKED";
                    case "UNCHECKED", "NOT_CHECKED", "UN-CHECKED" -> "ASSERT_UNCHECKED";
                    case "SELECTED" -> "ASSERT_SELECTED";
                    case "UNSELECTED", "NOT_SELECTED", "UN-SELECTED" -> "ASSERT_UNSELECTED";
                    case "FOCUSED" -> "ASSERT_FOCUSED";
                    case "EXISTS", "PRESENT" -> "ASSERT_EXISTS";
                    case "ABSENT", "NOT_EXIST", "NOT_EXISTS", "NON-EXISTENT" -> "ASSERT_ABSENT";
                    default -> "ASSERT";
                };
            }
            case "assert_attribute", "assert_attr" -> "ASSERT_ATTRIBUTE";
            case "press_key", "key_press" -> "KEY_PRESS";
            case "branch" -> "BRANCH";
            case "store" -> "STORE";
            case "include" -> "INCLUDE";
            case "execute_script" -> "EXECUTE_SCRIPT";
            default -> {
                if (args != null && args.hasNonNull("action") && !args.path("action").asText().isBlank())
                {
                    yield args.path("action").asText().toUpperCase(Locale.ROOT);
                }
                if (args != null && args.hasNonNull("type") && !args.path("type").asText().isBlank())
                {
                    yield args.path("type").asText().toUpperCase(Locale.ROOT);
                }
                yield name.toUpperCase(Locale.ROOT);
            }
        };

        String target = "";
        if (args != null && args.isObject())
        {
            if ("navigate".equals(name))
            {
                if (args.hasNonNull("url") && !args.path("url").asText().isBlank())
                {
                    target = args.path("url").asText();
                }
                else if (args.hasNonNull("target") && !args.path("target").asText().isBlank())
                {
                    target = args.path("target").asText();
                }
                else if (args.hasNonNull("locator") && !args.path("locator").asText().isBlank())
                {
                    target = args.path("locator").asText();
                }
                else if (args.hasNonNull("value") && !args.path("value").asText().isBlank())
                {
                    target = args.path("value").asText();
                }
            }
            else if ("execute_script".equals(name))
            {
                if (args.hasNonNull("script") && !args.path("script").asText().isBlank())
                {
                    target = args.path("script").asText();
                }
                else if (args.hasNonNull("target") && !args.path("target").asText().isBlank())
                {
                    target = args.path("target").asText();
                }
            }
            else if (args.hasNonNull("selector") && !args.path("selector").asText().isBlank())
            {
                target = args.path("selector").asText();
            }
            else if (args.hasNonNull("target") && !args.path("target").asText().isBlank())
            {
                target = args.path("target").asText();
            }
            else if (args.hasNonNull("locator") && !args.path("locator").asText().isBlank())
            {
                target = args.path("locator").asText();
            }
            else if (args.hasNonNull("url") && !args.path("url").asText().isBlank())
            {
                target = args.path("url").asText();
            }
            else if (args.hasNonNull("x") && args.hasNonNull("y"))
            {
                target = "coord: " + args.path("x").asInt() + "," + args.path("y").asInt();
            }
            else if (args.hasNonNull("text") && !args.path("text").asText().isBlank()
                    && ("click".equals(name) || "hover".equals(name)))
            {
                target = "text:" + args.path("text").asText();
            }
            else if ("assert_url".equals(name))
            {
                target = "url";
            }
            else if ("assert_title".equals(name))
            {
                target = "title";
            }
        }

        Object value = null;
        if (args != null && args.isObject())
        {
            if ("store".equals(name))
            {
                final String varName;
                if (args.hasNonNull("variableName") && !args.path("variableName").asText().isBlank())
                {
                    varName = args.path("variableName").asText();
                }
                else if (args.hasNonNull("variable") && !args.path("variable").asText().isBlank())
                {
                    varName = args.path("variable").asText();
                }
                else if (args.hasNonNull("name") && !args.path("name").asText().isBlank())
                {
                    varName = args.path("name").asText();
                }
                else if (args.hasNonNull("key") && !args.path("key").asText().isBlank())
                {
                    varName = args.path("key").asText();
                }
                else
                {
                    varName = "";
                }

                if (args.hasNonNull("value") && !args.path("value").asText().isBlank())
                {
                    value = List.of(varName, args.path("value").asText());
                }
                else
                {
                    value = varName;
                }
            }
            else if (args.hasNonNull("text") && !args.path("text").asText().isBlank())
            {
                value = args.path("text").asText();
            }
            else if (args.hasNonNull("value") && !args.path("value").asText().isBlank())
            {
                value = args.path("value").asText();
            }
            else if (args.hasNonNull("expectedText") && !args.path("expectedText").asText().isBlank())
            {
                value = args.path("expectedText").asText();
            }
            else if (args.hasNonNull("expectedUrl") && !args.path("expectedUrl").asText().isBlank())
            {
                value = args.path("expectedUrl").asText();
            }
            else if (args.hasNonNull("expectedTitle") && !args.path("expectedTitle").asText().isBlank())
            {
                value = args.path("expectedTitle").asText();
            }
            else if ("assert_element_state".equals(name) || "assert_state".equals(name))
            {
                if (args.hasNonNull("state") && !args.path("state").asText().isBlank())
                {
                    value = args.path("state").asText();
                }
            }
            else if ("assert_attribute".equals(name) || "assert_attr".equals(name))
            {
                final String attr = args.hasNonNull("attribute") ? args.path("attribute").asText()
                        : (args.hasNonNull("name") ? args.path("name").asText() : "");
                if (args.hasNonNull("expectedValue"))
                {
                    value = attr + "=" + args.path("expectedValue").asText();
                }
                else if (args.hasNonNull("value"))
                {
                    value = attr + "=" + args.path("value").asText();
                }
                else
                {
                    value = attr;
                }
            }
            else if (args.hasNonNull("title") && !args.path("title").asText().isBlank())
            {
                value = args.path("title").asText();
            }
            else if (args.hasNonNull("expectedCount"))
            {
                value = String.valueOf(args.path("expectedCount").asInt());
            }
            else if (args.hasNonNull("count"))
            {
                final int c = args.path("count").asInt();
                final String op = args.path("operator").asText("EXACT").toUpperCase(Locale.ROOT);
                if ("MIN".equals(op))
                {
                    value = ">=" + c;
                }
                else if ("MAX".equals(op))
                {
                    value = "<=" + c;
                }
                else
                {
                    value = String.valueOf(c);
                }
            }
            else if (args.hasNonNull("minCount"))
            {
                value = ">=" + args.path("minCount").asInt();
            }
            else if (args.hasNonNull("maxCount"))
            {
                value = "<=" + args.path("maxCount").asInt();
            }
            else if (args.hasNonNull("key") && !args.path("key").asText().isBlank())
            {
                value = args.path("key").asText();
            }
            else if (args.hasNonNull("durationMs") && !args.path("durationMs").asText().isBlank())
            {
                value = args.path("durationMs").asText();
            }
            else if (args.hasNonNull("time") && !args.path("time").asText().isBlank())
            {
                value = args.path("time").asText();
            }
            else if (args.hasNonNull("direction") && !args.path("direction").asText().isBlank())
            {
                value = args.path("direction").asText();
            }
            else if (args.hasNonNull("values") && args.path("values").isArray())
            {
                final List<String> valList = new ArrayList<>();
                for (final JsonNode node : args.path("values"))
                {
                    valList.add(node.asText());
                }
                value = valList;
            }
        }

        String description = "";
        if (args != null && args.hasNonNull("description") && !args.path("description").asText().isBlank())
        {
            description = args.path("description").asText();
        }
        else
        {
            description = switch (type)
            {
                case "NAVIGATE" -> "Navigate to " + target;
                case "CLICK" -> !target.isBlank() ? "Click " + target : "Click element";
                case "TYPE" -> "Type '" + (value != null ? value : "") + "' into " + target;
                case "ASSERT_TEXT" -> "Assert text '" + (value != null ? value : "") + "'" + (!target.isBlank() ? " on " + target : "");
                case "ASSERT_URL" -> "Assert URL '" + (value != null ? value : "") + "'";
                case "ASSERT_TITLE" -> "Assert page title '" + (value != null ? value : "") + "'";
                case "ASSERT_VISIBLE", "ASSERT_HIDDEN", "ASSERT_ENABLED", "ASSERT_DISABLED",
                     "ASSERT_EDITABLE", "ASSERT_READONLY", "ASSERT_CHECKED", "ASSERT_UNCHECKED",
                     "ASSERT_SELECTED", "ASSERT_UNSELECTED", "ASSERT_FOCUSED", "ASSERT_EXISTS",
                     "ASSERT_ABSENT" -> "Assert " + (!target.isBlank() ? target + " " : "") + "is " + (value != null ? value : type.substring("ASSERT_".length()).toLowerCase(Locale.ROOT));
                case "ASSERT_ATTRIBUTE" -> "Assert attribute '" + (value != null ? value : "") + "'" + (!target.isBlank() ? " on " + target : "");
                case "SELECT" -> "Select '" + (value != null ? value : "") + "' on " + target;
                case "HOVER" -> "Hover over " + target;
                case "KEY_PRESS" -> "Press key '" + (value != null ? value : "") + "'" + (!target.isBlank() ? " on " + target : "");
                case "SCROLL" -> "Scroll " + (!target.isBlank() ? target : (value != null ? value : ""));
                case "WAIT" -> "Wait " + (value != null ? value : "") + " ms";
                case "CLEAR" -> "Clear " + target;
                case "CLEAR_COOKIES" -> "Clear browser cookies";
                case "BACK" -> "Navigate back";
                case "FORWARD" -> "Navigate forward";
                case "REFRESH" -> "Refresh page";
                case "STORE" -> {
                    final String varName = value instanceof List<?> l && !l.isEmpty() ? String.valueOf(l.get(0)) : String.valueOf(value != null ? value : "");
                    yield !target.isBlank() ? "Store text from " + target + " as variable '" + varName + "'" : "Store variable '" + varName + "'";
                }
                default -> "Tool call: " + name;
            };
        }

        String reasoning = "";
        if (args != null && args.hasNonNull("reasoning") && !args.path("reasoning").asText().isBlank())
        {
            reasoning = args.path("reasoning").asText().trim();
        }
        else if (args != null && args.hasNonNull("thought") && !args.path("thought").asText().isBlank())
        {
            reasoning = args.path("thought").asText().trim();
        }

        final boolean isRegex = args != null && (args.path("regex").asBoolean(false)
                || (value != null && (value.toString().contains("[0-9]") || value.toString().contains("\\d")
                    || value.toString().contains(".*") || value.toString().contains(".+"))));

        final boolean adjust = args != null && args.path("adjust").asBoolean(false);

        final Action action = new Action(type, target, value, description, reasoning, isRegex);
        action.setAdjust(adjust);
        action.setToolCall(call);

        if (args != null && args.has("domFeatureVector") && !args.path("domFeatureVector").isNull())
        {
            try
            {
                final DomFeatureVector vector = MAPPER.treeToValue(args.path("domFeatureVector"), DomFeatureVector.class);
                action.setDomFeatureVector(vector);
            }
            catch (final Exception ignored)
            {
            }
        }

        return action;
    }
}
