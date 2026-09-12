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
            case "CLICK" -> "browser_click";
            case "TYPE" -> "browser_type";
            case "NAVIGATE", "OPEN" -> "browser_navigate";
            case "SELECT" -> "browser_select";
            case "HOVER" -> "browser_hover";
            case "ASSERT_TEXT", "ASSERT" -> "browser_assert_text";
            case "ASSERT_COUNT" -> "browser_assert_count";
            case "EXECUTE_SCRIPT", "SCRIPT" -> "browser_execute_script";
            case "SCROLL" -> "browser_scroll";
            default -> {
                if (this.type != null && this.type.startsWith("browser_"))
                {
                    yield this.type;
                }
                yield "browser_click";
            }
        };

        if ("browser_navigate".equals(toolName))
        {
            args.put("url", this.target != null ? this.target : "");
        }
        else if ("browser_assert_text".equals(toolName))
        {
            final String txt = (this.value != null && !this.value.isEmpty()) ? this.value.get(0) : this.target;
            args.put("text", txt != null ? txt : "");
        }
        else if ("browser_assert_count".equals(toolName))
        {
            args.put("selector", this.target != null ? this.target : "");
            if (this.value != null && !this.value.isEmpty())
            {
                final String v = this.value.get(0).trim();
                if (v.startsWith(">="))
                {
                    try
                    {
                        args.put("minCount", Integer.parseInt(v.substring(2).trim()));
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
                else if (v.startsWith("<="))
                {
                    try
                    {
                        args.put("maxCount", Integer.parseInt(v.substring(2).trim()));
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
                else
                {
                    try
                    {
                        args.put("expectedCount", Integer.parseInt(v));
                    }
                    catch (final NumberFormatException ignored)
                    {
                    }
                }
            }
        }
        else if ("browser_execute_script".equals(toolName))
        {
            args.put("script", this.target != null ? this.target : "");
        }
        else
        {
            args.put("target", this.target != null ? this.target : "");
            if (this.value != null && !this.value.isEmpty())
            {
                args.put("text", this.value.get(0));
                args.put("value", this.value.get(0));
            }
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

        final String name = call.toolName() != null ? call.toolName() : "";
        final JsonNode args = call.arguments();
        final String type = switch (name)
        {
            case "browser_navigate" -> "NAVIGATE";
            case "browser_click" -> "CLICK";
            case "browser_type" -> "TYPE";
            case "browser_hover" -> "HOVER";
            case "browser_scroll" -> "SCROLL";
            case "browser_select" -> "SELECT";
            case "browser_clear" -> "CLEAR";
            case "browser_clear_cookies" -> "CLEAR_COOKIES";
            case "browser_back" -> "BACK";
            case "browser_forward" -> "FORWARD";
            case "browser_refresh" -> "REFRESH";
            case "browser_wait" -> "WAIT";
            case "browser_assert_text" -> "ASSERT_TEXT";
            case "browser_assert_count" -> "ASSERT_COUNT";
            case "browser_press_key" -> "KEY_PRESS";
            case "browser_branch" -> "BRANCH";
            case "browser_store" -> "STORE";
            case "browser_include" -> "INCLUDE";
            case "browser_execute_script" -> "EXECUTE_SCRIPT";
            default -> {
                if (args != null && args.hasNonNull("action") && !args.path("action").asText().isBlank())
                {
                    yield args.path("action").asText().toUpperCase(Locale.ROOT);
                }
                if (args != null && args.hasNonNull("type") && !args.path("type").asText().isBlank())
                {
                    yield args.path("type").asText().toUpperCase(Locale.ROOT);
                }
                yield name.startsWith("browser_") ? name.substring("browser_".length()).toUpperCase(Locale.ROOT) : name.toUpperCase(Locale.ROOT);
            }
        };

        String target = "";
        if (args != null && args.isObject())
        {
            if ("browser_navigate".equals(name))
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
            else if ("browser_execute_script".equals(name))
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
                    && ("browser_click".equals(name) || "browser_hover".equals(name)))
            {
                target = "text:" + args.path("text").asText();
            }
        }

        Object value = null;
        if (args != null && args.isObject())
        {
            if (args.hasNonNull("text") && !args.path("text").asText().isBlank())
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
            else if (args.hasNonNull("expectedCount"))
            {
                value = String.valueOf(args.path("expectedCount").asInt());
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

        final Action action = new Action(type, target, value, description, reasoning, isRegex);
        action.setToolCall(call);
        return action;
    }
}
