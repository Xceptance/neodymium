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
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.neodymium.ai.model.DomFeatureVector;

/**
 * Represents a single executable action parsed from LLM response or recording.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Action
{
    private List<Action> condition;
    private List<Action> then;
    @JsonProperty("else")
    private List<Action> elseActions;
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
            @JsonProperty(value = "target", required = false) @JsonAlias({"locator", "target"}) final String target,
            @JsonProperty(value = "value", required = false) @JsonAlias({"values", "value"}) final Object value,
            @JsonProperty(value = "description", required = false) final String description,
            @JsonProperty(value = "reasoning", required = false) final String reasoning,
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
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = this.domFeatureVector;
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
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = this.domFeatureVector;
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
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = this.domFeatureVector;
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
        copy.adjust = this.adjust;
        copy.stepInstruction = this.stepInstruction;
        copy.stepLine = this.stepLine;
        copy.stepFile = this.stepFile;
        copy.stepScreenshotHash = this.stepScreenshotHash;
        copy.selfCritique = this.selfCritique;
        copy.candidateLocators = new ArrayList<>(this.candidateLocators);
        copy.domFeatureVector = featureVector;
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
}
