package com.xceptance.neodymium.ai.action;

/**
 * Types of browser actions that the AI agent can execute.
 */
public enum ActionType
{
    /** Click on an element */
    CLICK,

    /** Type text into an input field */
    TYPE,

    /** Navigate to a URL */
    NAVIGATE,

    /** Select an option from a dropdown */
    SELECT,

    /** Wait for a specified duration or condition */
    WAIT,

    /** Assert that an element contains expected text or is in an expected state */
    ASSERT,

    /** Scroll to an element or position */
    SCROLL,

    /** Press a keyboard key (e.g., ENTER, TAB) */
    KEY_PRESS,

    /** Clear an input field before typing */
    CLEAR,

    /** Hover over an element */
    HOVER,

    /** Navigate back in browser history */
    BACK,

    /** Navigate forward in browser history */
    FORWARD,

    /** Refresh the current page */
    REFRESH,

    /** Clear browser cookies */
    CLEAR_COOKIES,

    /**
     * Invoke a Java method via reflection.
     * target = fully-qualified "ClassName.methodName" (static) or "ClassName" for an
     * instance obtained via a no-arg constructor.
     * value  = the single String argument passed to the method.
     */
    JAVA_METHOD
}
