/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.action;

import java.util.List;
import java.util.HashMap;
import com.google.gson.annotations.JsonAdapter;

/**
 * Represents a single browser action parsed from the LLM's response. Each action maps to a concrete Selenium
 * interaction.
  *
 * // AI-generated: Gemini 2.0 Flash
 */
public class Action
{
    private String type;

    private String target;

    @JsonAdapter(ValueListTypeAdapter.class)
    private List<String> value;

    private String description;

    private java.util.Map<String, String> dataBindings;

    private String elementDetails;

    private String reasoning;

    private java.util.Map<String, String> elementContext;

    private String screenshotPath;

    private String replay = "";

    private List<Action> condition;

    private List<Action> then;

    @com.google.gson.annotations.SerializedName("else")
    private List<Action> elseActions;

    private transient boolean silent = false;

    private boolean adjust = false;

    public Action()
    {
    }

    public Action(final String type, final String target, final String description)
    {
        this.type = type;
        this.target = target;
        this.value = null;
        this.description = description;
    }

    public Action(final String type, final String target, final String value, final String description)
    {
        this(type, target, value != null ? List.of(value) : null, description);
    }

    public Action(final String type, final String target, final List<String> value, final String description) {
        this.type = type;
        this.target = target;
        this.value = value;
        this.description = description;
        this.elementContext = new HashMap<String, String>();
    }

    public void markReplay() {
        this.replay = "(Replay)";
    }

    public String getType()
    {
        return type;
    }

    public void setType(final String type)
    {
        this.type = type;
    }

    /**
     * Gets the plugin responsible for executing this action, based on the action type.
     */
    public AiActionPlugin getPlugin() {
        return ActionRegistry.getPlugin(type);
    }

    /**
     * The target element — a CSS selector, XPath, data-ai-id reference, or text label.
     */
    public String getTarget()
    {
        return target;
    }

    public void setTarget(final String target)
    {
        this.target = target;
    }

    /**
     * The value for the action — text to type, URL to navigate to, expected assertion text, key to press, etc.
     */
    public String getValue()
    {
        return value != null && !value.isEmpty() ? value.get(0) : null;
    }

    public List<String> getValues()
    {
        return value;
    }

    public void setValue(final List<String> value)
    {
        this.value = value;
    }

    /**
     * A human-readable description of what this action does.
     */
    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public java.util.Map<String, String> getDataBindings() {
        return dataBindings;
    }

    public void setDataBindings(final java.util.Map<String, String> dataBindings) {
        this.dataBindings = dataBindings;
    }

    public String getElementDetails() {
        return elementDetails;
    }

    public void setElementDetails(final String elementDetails) {
        this.elementDetails = elementDetails;
    }

    public String getReasoning()
    {
        return reasoning;
    }

    public void setReasoning(final String reasoning)
    {
        this.reasoning = reasoning;
    }

    public java.util.Map<String, String> getElementContext()
    {
        return elementContext;
    }

    public void setElementContext(final java.util.Map<String, String> elementContext)
    {
        this.elementContext = elementContext;
    }

    @Override
    public String toString()
    {
        return String.format("Action{type=%s, target='%s', value='%s', desc='%s', elementDetails='%s', reasoning='%s', context=%s, adjust=%b}",
                             type, target, value, description, elementDetails, reasoning, elementContext != null ? "yes" : "no", adjust);
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public String getReplay() {
        return replay;
    }

    public List<Action> getCondition() {
        return condition;
    }

    public void setCondition(List<Action> condition) {
        this.condition = condition;
    }

    public List<Action> getThen() {
        return then;
    }

    public void setThen(List<Action> then) {
        this.then = then;
    }

    public List<Action> getElseActions() {
        return elseActions;
    }

    public void setElseActions(List<Action> elseActions) {
        this.elseActions = elseActions;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean getAdjust()
    {
        return adjust;
    }

    public void setAdjust(final boolean adjust)
    {
        this.adjust = adjust;
    }
}
