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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern JAVA_METHOD_PATTERN = Pattern.compile(
        "(?i)java:\\s*([a-zA-Z_][a-zA-Z0-9_]*)(?:\\(\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|[^)]*)\\s*\\))?"
    );

    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        final Matcher matcher = JAVA_METHOD_PATTERN.matcher(instruction);
        if (matcher.find())
        {
            final String method = matcher.group(1);
            final String rawParam = matcher.group(2);
            final String param;
            if (rawParam != null)
            {
                final String trimmed = rawParam.strip();
                if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'")))
                {
                    param = trimmed.substring(1, trimmed.length() - 1);
                }
                else
                {
                    param = trimmed;
                }
            }
            else
            {
                param = null;
            }

            final List<String> values = param != null ? List.of(param) : List.of();

            LOG.debug("▶️ [EXEC] Direct call Java Method {} with param {}", method, param);
            return List.of(new Action("JAVA_METHOD", method, values,
                    "Call " + method + " with param " + param));
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
                + "tg = the simple method name (no class name or dots). "
                + "v = the single String argument passed to the method.";
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
        sb.append("\nAvailable utility methods you can invoke:");

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
                            sb.append("\n- ").append(method.getName()).append("(").append(paramStr).append(")");
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
            case "assertNumbersEqual" -> "Asserts that the first number is equal to the second. Expects two values.";
            case "assertMatchesRegex" -> "Asserts that the provided value matches the given regular expression pattern. Expects two values (e.g. 'ORD-123, ^ORD-[0-9]{3}$' or '[\"ORD-123\", \"^ORD-[0-9]{3}$\"]').";
            case "assertCalculation" -> "Asserts that the mathematical equation is correct within an allowed tolerance of 0.02 (e.g. '0,90 € = (14,96 € + 0,00 €) * 6,00%').";
            default -> "";
        };
    }

    /**
     * Compiles a combined map of all available Java methods (from utility classes and optionally
     * the test class) with their signatures and descriptions. Used by JIT PESAP to present
     * the full method catalog for analysis.
     *
     * @param testClass the active test class to scan for instance methods, or {@code null} to skip
     * @return a map of method name to formatted signature+description string
     */
    public final Map<String, String> getAllAvailableMethods(final Class<?> testClass)
    {
        final Map<String, String> methods = new LinkedHashMap<>();

        // Scan utility classes for public static methods
        final List<String> utilityClassNames = Neodymium.aiConfiguration().aiJavaMethodUtilityClasses();
        if (utilityClassNames != null)
        {
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
                    collectMethods(clazz, true, methods);
                }
                catch (final Exception e)
                {
                    // Ignore classpath load issues during prompt construction
                }
            }
        }

        // Scan test class for public instance methods (not inherited from Object)
        if (testClass != null)
        {
            collectMethods(testClass, false, methods);
        }

        return methods;
    }

    /**
     * Collects eligible methods from a class into the provided map.
     * For utility classes, only {@code public static} methods are collected.
     * For test classes, {@code public} methods (both static and non-static) declared on the class itself
     * (not inherited from {@code Object}) are collected.
     *
     * @param clazz       the class to scan
     * @param staticOnly  if {@code true}, only collect static methods
     * @param methods     the map to populate with method name → description
     */
    private void collectMethods(final Class<?> clazz, final boolean staticOnly,
            final Map<String, String> methods)
    {
        final Method[] declaredMethods = staticOnly ? clazz.getMethods() : clazz.getDeclaredMethods();
        for (final Method method : declaredMethods)
        {
            final int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers))
            {
                continue;
            }
            if (staticOnly && !Modifier.isStatic(modifiers))
            {
                continue;
            }

            // Skip Object inherited methods for test classes
            if (!staticOnly && method.getDeclaringClass() == Object.class)
            {
                continue;
            }

            final Class<?>[] params = method.getParameterTypes();
            if (params.length == 0 || (params.length == 1 && params[0] == String.class))
            {
                final String paramStr = params.length == 0 ? "" : "String";
                final String desc = getMethodExplanation(method.getName());
                final String formatted = method.getName() + "(" + paramStr + ")"
                        + (desc.isEmpty() ? "" : ": " + desc);
                methods.putIfAbsent(method.getName(), formatted);
            }
        }
    }

    /**
     * Constructs prompt instructions listing only the targeted methods predicted by JIT PESAP.
     * If {@code targetedMethods} is {@code null} or empty, falls back to listing all methods
     * (legacy behavior).
     *
     * @param testClass       the active test class to scan for instance methods, or {@code null}
     * @param targetedMethods the set of method names to include, or {@code null}/empty for all
     * @return the formatted prompt instructions string
     */
    public final String getPromptInstructions(final Class<?> testClass, final Set<String> targetedMethods)
    {
        final StringBuilder instructions = new StringBuilder();
        instructions.append("JAVA_METHOD: Invoke a Java method on the current test instance class via reflection. ")
                .append("tg = the simple method name (no class name or dots). ")
                .append("v = the single String argument passed to the method.");

        if (testClass == null)
        {
            return instructions.toString();
        }

        final Map<String, String> allMethods = getAllAvailableMethods(testClass);

        if (allMethods.isEmpty())
        {
            return instructions.toString();
        }


        // Filter to targeted methods if provided
        final Map<String, String> methodsToList;
        if (targetedMethods != null && !targetedMethods.isEmpty())
        {
            methodsToList = new LinkedHashMap<>();
            for (final String name : targetedMethods)
            {
                final String desc = allMethods.get(name);
                if (desc != null)
                {
                    methodsToList.put(name, desc);
                }
            }
        }
        else
        {
            methodsToList = allMethods;
        }

        if (!methodsToList.isEmpty())
        {
            instructions.append("\nAvailable methods for this step:");
            for (final Map.Entry<String, String> entry : methodsToList.entrySet())
            {
                instructions.append("\n- ").append(entry.getValue());
            }
        }

        return instructions.toString();
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

        final int paramCount = param.length;

        // Stage 1: Try the test instance class
        final MethodMatch instanceMatch = findMethod(testInstance.getClass(), methodName, paramCount);
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
                final MethodMatch utilityMatch = findMethod(utilityClass, methodName, paramCount);
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
     * Prefers {@code methodName(String)} over {@code methodName()} if parameters are present,
     * otherwise prefers {@code methodName()} over {@code methodName(String)}.
     *
     * @param clazz      the class to search
     * @param methodName the method name
     * @param paramCount the number of parameters passed
     * @return a {@link MethodMatch} if found, or {@code null}
     */
    private static MethodMatch findMethod(final Class<?> clazz, final String methodName, final int paramCount)
    {
        if (paramCount == 0)
        {
            try
            {
                final Method method = clazz.getMethod(methodName);
                return new MethodMatch(method, false);
            }
            catch (final NoSuchMethodException e)
            {
                try
                {
                    final Method method = clazz.getMethod(methodName, String.class);
                    return new MethodMatch(method, true);
                }
                catch (final NoSuchMethodException ex)
                {
                    return null;
                }
            }
        }
        else
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
                catch (final NoSuchMethodException ex)
                {
                    return null;
                }
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
