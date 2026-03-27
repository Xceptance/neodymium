package com.xceptance.neodymium.ai.action;

/**
 * Represents a single browser action parsed from the LLM's response.
 * Each action maps to a concrete Selenium interaction.
 */
public class Action {
    private ActionType type;
    private String target;
    private String value;
    private String description;

    private String elementDetails;

    public Action() {
    }

    public Action(final ActionType type, final String target, final String value, final String description) {
        this.type = type;
        this.target = target;
        this.value = value;
        this.description = description;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(final ActionType type) {
        this.type = type;
    }

    /**
     * The target element — a CSS selector, XPath, data-ai-id reference, or text
     * label.
     */
    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    /**
     * The value for the action — text to type, URL to navigate to,
     * expected assertion text, key to press, etc.
     */
    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * A human-readable description of what this action does.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getElementDetails()
    {
        return elementDetails;
    }

    public void setElementDetails(final String elementDetails)
    {
        this.elementDetails = elementDetails;
    }


    @Override
    public String toString() {
        return String.format("Action{type=%s, target='%s', value='%s', desc='%s', elementDetails='%s'}",
                             type, target, value, description, elementDetails);
    }
}
