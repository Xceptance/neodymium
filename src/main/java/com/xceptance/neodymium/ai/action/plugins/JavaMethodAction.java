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
 *
 * // AI-generated: Claude Opus 4.6
 */
package com.xceptance.neodymium.ai.action.plugins;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Plugin action that invokes a Java method by name via reflection.
 * <p>
 * Method resolution follows a two-stage fallback strategy:
 * <ol>
 *   <li>Search the active test instance class for a matching public method.</li>
 *   <li>If not found, scan all registered utility classes (configured via
 *       {@code neodymium.ai.agent.javaMethod.utilityClasses}) for a matching
 *       {@code public static} method.</li>
 * </ol>
 *
 * @see com.xceptance.neodymium.ai.util.AiAssertions
 * @see com.xceptance.neodymium.ai.config.AiConfiguration#aiJavaMethodUtilityClasses()
 */
public class JavaMethodAction implements AiActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(JavaMethodAction.class);

    @Override
    public String getActionName()
    {
        return "JAVA_METHOD";
    }

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        if (instruction.toLowerCase().contains("java"))
        {
            final String patternStr = Neodymium.configuration().getProperty(
                    "neodymium.ai.agent.pattern.javaMethod",
                    "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)");
            final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(patternStr)
                    .matcher(instruction.strip());
            if (matcher.find())
            {
                final String method = matcher.group(1);
                final String param = matcher.group(2);
                LOG.debug("▶️ [EXEC] Direct call Java Method {} with param {}", method, param);
                return List.of(new Action("JAVA_METHOD", method, List.of(param),
                        "Call " + method + " with param " + param));
            }
        }
        return null;
    }

    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }

    @Override
    public String getPromptInstructions()
    {
        return "JAVA_METHOD: Invoke a Java method on the current test instance class via reflection. "
                + "target = the simple method name (no class name or dots). "
                + "value = the single String argument passed to the method.";
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final String target = action.getTarget();
        if (target == null || target.isBlank())
        {
            throw new ActionExecutor.ActionExecutionException(
                    "JAVA_METHOD action requires a 'target' containing the method name");
        }

        final int lastDot = target.lastIndexOf('.');
        if (lastDot >= 0)
        {
            throw new ActionExecutor.ActionExecutionException(
                    "JAVA_METHOD target must be a simple method name (no dots), got: " + target);
        }

        final String methodName = target;
        final Object[] param = action.getValues().toArray();

        LOG.debug("JAVA_METHOD: method='{}', param='{}'", methodName, param);

        // Stage 1: Try the test instance class
        final MethodMatch instanceMatch = findMethod(testInstance.getClass(), methodName);
        if (instanceMatch != null)
        {
            invokeMethod(instanceMatch, testInstance, methodName, param);
            return;
        }

        // Stage 2: Scan registered utility classes
        final List<String> utilityClassNames = Neodymium.aiConfiguration().aiJavaMethodUtilityClasses();
        for (final String className : utilityClassNames)
        {
            final String trimmed = className.trim();
            if (trimmed.isEmpty())
            {
                continue;
            }

            try
            {
                final Class<?> utilityClass = Class.forName(trimmed);
                final MethodMatch utilityMatch = findMethod(utilityClass, methodName);
                if (utilityMatch != null)
                {
                    if (!Modifier.isStatic(utilityMatch.method.getModifiers()))
                    {
                        LOG.warn("JAVA_METHOD: Found '{}' in utility class '{}' but it is not static, skipping.",
                                methodName, trimmed);
                        continue;
                    }
                    LOG.debug("JAVA_METHOD: Resolved '{}' from utility class '{}'", methodName, trimmed);
                    invokeMethod(utilityMatch, null, methodName, param);
                    return;
                }
            }
            catch (final ClassNotFoundException e)
            {
                LOG.warn("JAVA_METHOD: Configured utility class '{}' not found on classpath, skipping.", trimmed);
            }
        }

        // Nothing found anywhere
        throw new ActionExecutor.ActionExecutionException(
                String.format("JAVA_METHOD: no public method '%s(String)' found on class '%s' or any registered "
                                + "utility class %s",
                        methodName, testInstance.getClass().getSimpleName(), utilityClassNames));
    }

    /**
     * Searches the given class for a public method with the specified name.
     * Prefers {@code methodName(String)} over {@code methodName()}.
     *
     * @param clazz      the class to search
     * @param methodName the method name
     * @return a {@link MethodMatch} if found, or {@code null}
     */
    private static MethodMatch findMethod(final Class<?> clazz, final String methodName)
    {
        try
        {
            final Method method = clazz.getMethod(methodName, String.class);
            return new MethodMatch(method, true);
        }
        catch (final NoSuchMethodException e)
        {
            try
            {
                final Method method = clazz.getMethod(methodName);
                return new MethodMatch(method, false);
            }
            catch (final NoSuchMethodException e1)
            {
                return null;
            }
        }
    }

    /**
     * Invokes the resolved method on the given instance (or statically if instance is null).
     *
     * @param match      the resolved method match
     * @param instance   the object instance to invoke on (null for static calls)
     * @param methodName the method name (for error messages)
     * @param param      the parameters to pass
     */
    private static void invokeMethod(final MethodMatch match, final Object instance,
            final String methodName, final Object[] param)
    {
        try
        {
            final boolean isStatic = Modifier.isStatic(match.method.getModifiers());
            final Object target = isStatic ? null : instance;

            if (match.hasParam)
            {
                LOG.debug("Invoking {}method {}(\"{}\")", isStatic ? "static " : "", methodName, param);
                match.method.invoke(target, param);
            }
            else
            {
                LOG.debug("Invoking {}method {}()", isStatic ? "static " : "", methodName);
                match.method.invoke(target);
            }
        }
        catch (final java.lang.reflect.InvocationTargetException e)
        {
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ActionExecutor.ActionExecutionException(
                    String.format("JAVA_METHOD: '%s' threw an exception: %s", methodName, cause.getMessage()), cause);
        }
        catch (final Exception e)
        {
            throw new ActionExecutor.ActionExecutionException(
                    String.format("JAVA_METHOD: failed to invoke '%s': %s", methodName, e.getMessage()), e);
        }
    }

    /**
     * Simple holder for a resolved method and whether it accepts a String parameter.
     */
    private static final class MethodMatch
    {
        final Method method;
        final boolean hasParam;

        MethodMatch(final Method method, final boolean hasParam)
        {
            this.method = method;
            this.hasParam = hasParam;
        }
    }
}
