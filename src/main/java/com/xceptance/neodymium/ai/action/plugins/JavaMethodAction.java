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
package com.xceptance.neodymium.ai.action.plugins;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

    private volatile String cachedUtilityMethodsDescription = null;

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
        final StringBuilder instructions = new StringBuilder();
        instructions.append("JAVA_METHOD: Invoke a Java method on the current test instance class via reflection. ")
                .append("target = the simple method name (no class name or dots). ")
                .append("value = the single String argument passed to the method.");

        if (cachedUtilityMethodsDescription == null)
        {
            synchronized (this)
            {
                if (cachedUtilityMethodsDescription == null)
                {
                    cachedUtilityMethodsDescription = getUtilityMethodsDescription();
                }
            }
        }

        if (!cachedUtilityMethodsDescription.isEmpty())
        {
            instructions.append(cachedUtilityMethodsDescription);
        }

        return instructions.toString();
    }

    /**
     * Reflectively scans all configured utility classes for public static methods
     * that can be invoked via the JAVA_METHOD action and formats their signatures and descriptions.
     *
     * @return a formatted string listing available utility methods, or an empty string
     */
    private String getUtilityMethodsDescription()
    {
        final List<String> utilityClassNames = Neodymium.aiConfiguration().aiJavaMethodUtilityClasses();
        if (utilityClassNames == null || utilityClassNames.isEmpty())
        {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("\n  Available utility methods you can invoke:");

        for (final String className : utilityClassNames)
        {
            final String trimmed = className.trim();
            if (trimmed.isEmpty())
            {
                continue;
            }

            try
            {
                final Class<?> clazz = Class.forName(trimmed);
                final Method[] methods = clazz.getMethods();

                for (final Method method : methods)
                {
                    final int modifiers = method.getModifiers();
                    if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))
                    {
                        final Class<?>[] params = method.getParameterTypes();
                        if (params.length == 0 || (params.length == 1 && params[0] == String.class))
                        {
                            final String paramStr = params.length == 0 ? "" : "String";
                            final String desc = getMethodExplanation(method.getName());
                            sb.append("\n    - ").append(method.getName()).append("(").append(paramStr).append(")");
                            if (!desc.isEmpty())
                            {
                                sb.append(": ").append(desc);
                            }
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                // Ignore classpath load issues for prompt construction
            }
        }
        return sb.toString();
    }

    /**
     * Returns a concise description for the built-in assertion methods to guide the LLM's selection.
     *
     * @param methodName the name of the method
     * @return the description string, or empty string if not a built-in method
     */
    private String getMethodExplanation(final String methodName)
    {
        return switch (methodName)
        {
            case "assertPriceGreaterThanZero" -> "Asserts that the price string (any locale/currency, e.g. '$17.99', '12,50 €') represents a value strictly greater than zero.";
            case "assertGreaterThanZero" -> "Asserts that the numeric/price string represents a value strictly greater than zero.";
            case "verifyLessOrEqual" -> "Verifies that the first extracted number is less than or equal to the second. Expects a JSON array with exactly two values (e.g. '[\"10\", \"15\"]').";
            case "assertNumberGreaterThan" -> "Asserts that the first number is strictly greater than the second. Expects a comma-separated string or a JSON array of two values (e.g. '15.00, 10.00' or '[\"15.00\", \"10.00\"]').";
            case "assertNumberGreaterThanOrEqual" -> "Asserts that the first number is greater than or equal to the second. Expects two values.";
            case "assertNumberLessThan" -> "Asserts that the first number is strictly less than the second. Expects two values.";
            case "assertNumberLessThanOrEqual" -> "Asserts that the first number is less than or equal to the second. Expects two values.";
            case "assertNumberEqual" -> "Asserts that the first number is equal to the second. Expects two values.";
            case "assertMatchesRegex" -> "Asserts that the provided value matches the given regular expression pattern. Expects two values (e.g. 'ORD-123, ^ORD-[0-9]{3}$' or '[\"ORD-123\", \"^ORD-[0-9]{3}$\"]').";
            case "verifyCalculation" -> "Verifies that the mathematical equation is correct within an allowed tolerance of 0.02 (e.g. '0,90 € = (14,96 € + 0,00 €) * 6,00%').";
            default -> "";
        };
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
        final List<String> values = action.getValues();
        final Object[] param = values != null ? values.toArray() : new Object[0];

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
