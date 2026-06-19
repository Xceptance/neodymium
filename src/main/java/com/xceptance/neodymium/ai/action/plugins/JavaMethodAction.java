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
import com.xceptance.neodymium.ai.core.AiBrowser;
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

    private static final Pattern JAVA_METHOD_PATTERN = Pattern.compile(
        "(?i)java:\\s*([a-zA-Z_][a-zA-Z0-9_]*)(?:\\(\\s*(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|[^)]*)\\s*\\))?"
    );

    // Thread-safe cache of static configuration classes scanned from classes/packages configs
    private static final Set<Class<?>> staticConfigurationClasses = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Map<String, String> staticConfigurationMethods = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile boolean configurationScanned = false;
    private static List<String> lastScannedClasses = null;
    private static List<String> lastScannedPackages = null;

    @Override
    public String getActionName()
    {
        return "JAVA_METHOD";
    }

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
        return "JAVA_METHOD: Invoke a public Java method on the current test class or configuration helper via reflection. Set 'tg' to the simple method name (no dots or class prefixes) and 'v' to the string argument (if the method accepts one).";
    }

    /**
     * Resolves the configured utility/method classes based on configuration,
     * supporting fallback to the old configuration key if customized.
     *
     * @return list of class names
     */
    private static List<String> getResolvedClasses()
    {
        final List<String> classes = Neodymium.aiConfiguration().aiJavaMethodClasses();
        final List<String> utilityClasses = Neodymium.aiConfiguration().aiJavaMethodUtilityClasses();

        // If utilityClasses is customized (different from default) and classes is default, fallback to utilityClasses
        final boolean isUtilityCustomized = utilityClasses != null && (utilityClasses.size() != 1 || !utilityClasses.get(0).equals("com.xceptance.neodymium.ai.util.AiAssertions"));
        final boolean isClassesDefault = classes == null || (classes.size() == 1 && classes.get(0).equals("com.xceptance.neodymium.ai.util.AiAssertions"));

        if (isUtilityCustomized && isClassesDefault)
        {
            return utilityClasses;
        }
        return classes;
    }

    private static void scanConfigurationIfNeeded()
    {
        final List<String> classes = getResolvedClasses();
        final List<String> packages = Neodymium.aiConfiguration().aiJavaMethodPackages();

        if (configurationScanned && classes.equals(lastScannedClasses) && packages.equals(lastScannedPackages))
        {
            return;
        }

        synchronized (JavaMethodAction.class)
        {
            if (configurationScanned && classes.equals(lastScannedClasses) && packages.equals(lastScannedPackages))
            {
                return;
            }

            staticConfigurationClasses.clear();
            staticConfigurationMethods.clear();

            if (classes != null)
            {
                for (final String className : classes)
                {
                    final String trimmed = className.trim();
                    if (!trimmed.isEmpty())
                    {
                        try
                        {
                            staticConfigurationClasses.add(Class.forName(trimmed));
                        }
                        catch (final ClassNotFoundException e)
                        {
                            LOG.warn("Configured utility class '{}' not found on classpath.", trimmed);
                        }
                    }
                }
            }

            if (packages != null)
            {
                for (final String pkgName : packages)
                {
                    final String trimmed = pkgName.trim();
                    if (!trimmed.isEmpty())
                    {
                        for (final Class<?> clazz : getClassesInPackage(trimmed))
                        {
                            staticConfigurationClasses.add(clazz);
                        }
                    }
                }
            }

            // Populate the static configuration methods map (must be annotated with @AiMethod)
            for (final Class<?> clazz : staticConfigurationClasses)
            {
                final Method[] methods = clazz.getMethods();
                for (final Method method : methods)
                {
                    final int modifiers = method.getModifiers();
                    if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && method.isAnnotationPresent(AiMethod.class))
                    {
                        final String sig = formatMethodSignature(method);
                        final String desc = method.getAnnotation(AiMethod.class).value();
                        final String formatted = sig + (desc.isEmpty() ? "" : ": " + desc);
                        staticConfigurationMethods.putIfAbsent(method.getName(), formatted);
                    }
                }
            }

            lastScannedClasses = new ArrayList<>(classes);
            lastScannedPackages = new ArrayList<>(packages);
            configurationScanned = true;
        }
    }

    /**
     * Compiles a combined map of all available Java methods (from utility classes, packages,
     * dynamically registered classes, and optionally the test class) with their signatures and descriptions.
     *
     * @param testClass the active test class to scan for instance methods, or {@code null} to skip
     * @return a map of method name to formatted signature+description string
     */
    public final Map<String, String> getAllAvailableMethods(final Class<?> testClass)
    {
        scanConfigurationIfNeeded();

        final Map<String, String> methods = new LinkedHashMap<>();

        // 1. Add cached static methods from configured classes and packages
        methods.putAll(staticConfigurationMethods);

        // 2. Scan dynamically registered classes from active AiBrowser instance (thread-local context)
        try
        {
            final AiBrowser ai = Neodymium.ai();
            if (ai != null)
            {
                final List<Class<?>> dynamicClasses = ai.getDynamicallyRegisteredClasses();
                for (final Class<?> clazz : dynamicClasses)
                {
                    collectMethods(clazz, true, methods);
                }
            }
        }
        catch (final Exception e)
        {
            // Thread-local browser context might not be active, ignore
        }

        // 3. Scan test class for public instance/static methods annotated with @AiMethod
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
    private void collectMethods(final Class<?> clazz, final boolean staticOnly, final Map<String, String> methods)
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

            // Skip Object inherited methods
            if (method.getDeclaringClass() == Object.class)
            {
                continue;
            }

            // MUST be annotated with @AiMethod
            if (!method.isAnnotationPresent(AiMethod.class))
            {
                continue;
            }

            final String sig = formatMethodSignature(method);
            final String desc = method.getAnnotation(AiMethod.class).value();
            final String formatted = sig + (desc.isEmpty() ? "" : ": " + desc);
            methods.putIfAbsent(method.getName(), formatted);
        }
    }

    /**
     * Formats a method signature to include return type and parameter types.
     *
     * @param method the method to format
     * @return the formatted signature string
     */
    private static String formatMethodSignature(final Method method)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(method.getReturnType().getSimpleName()).append(" ").append(method.getName()).append("(");
        final Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++)
        {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1)
            {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Constructs prompt instructions listing only the targeted methods predicted by JIT PESAP.
     *
     * @param testClass       the active test class to scan, or {@code null}
     * @param targetedMethods the set of method names to include, or {@code null}/empty for all
     * @return the formatted prompt instructions string
     */
    public final String getPromptInstructions(final Class<?> testClass, final Set<String> targetedMethods)
    {
        final StringBuilder instructions = new StringBuilder();
        instructions.append("JAVA_METHOD: Invoke a public Java method on the current test class or configuration helper via reflection. Set 'tg' to the simple method name (no dots or class prefixes) and 'v' to the string argument (if the method accepts one).");

        if (testClass == null && targetedMethods == null)
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
            instructions.append("\n  Available methods for this step:");
            for (final Map.Entry<String, String> entry : methodsToList.entrySet())
            {
                instructions.append("\n  - ").append(entry.getValue());
            }
        }

        return instructions.toString();
    }

    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        scanConfigurationIfNeeded();

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

        LOG.debug("JAVA_METHOD: method='{}', values='{}'", methodName, values);

        // Stage 1: Try the test instance class
        if (testInstance != null)
        {
            final ResolvedMethodCall instanceCall = resolveMethod(testInstance.getClass(), methodName, values, false);
            if (instanceCall != null)
            {
                invokeMethod(instanceCall, testInstance, methodName);
                return;
            }
        }

        // Stage 2: Try dynamically registered classes
        try
        {
            final AiBrowser ai = Neodymium.ai();
            if (ai != null)
            {
                final List<Class<?>> dynamicClasses = ai.getDynamicallyRegisteredClasses();
                for (final Class<?> clazz : dynamicClasses)
                {
                    final ResolvedMethodCall dynamicCall = resolveMethod(clazz, methodName, values, true);
                    if (dynamicCall != null)
                    {
                        LOG.debug("JAVA_METHOD: Resolved '{}' from dynamically registered class '{}'", methodName, clazz.getName());
                        invokeMethod(dynamicCall, null, methodName);
                        return;
                    }
                }
            }
        }
        catch (final Exception e)
        {
            // Thread-local context might not be active, ignore
        }

        // Stage 3: Scan registered/cached static utility classes and packages
        for (final Class<?> utilityClass : staticConfigurationClasses)
        {
            final ResolvedMethodCall utilityCall = resolveMethod(utilityClass, methodName, values, true);
            if (utilityCall != null)
            {
                LOG.debug("JAVA_METHOD: Resolved '{}' from utility class '{}'", methodName, utilityClass.getName());
                invokeMethod(utilityCall, null, methodName);
                return;
            }
        }

        // Nothing found anywhere
        throw new ActionExecutor.ActionExecutionException(
                String.format("JAVA_METHOD: no public method '%s' annotated with @AiMethod found on class '%s', dynamically registered classes, or configured utility classes/packages",
                        methodName, testInstance != null ? testInstance.getClass().getSimpleName() : "null"));
    }

    /**
     * Resolves a method from the target class that matches the name and whose arguments can be converted successfully.
     *
     * @param clazz      the class to search
     * @param methodName the name of the method
     * @param values     the list of raw values
     * @param staticOnly whether to restrict search to static methods
     * @return a ResolvedMethodCall or null
     */
    private static ResolvedMethodCall resolveMethod(final Class<?> clazz, final String methodName, final List<String> values, final boolean staticOnly)
    {
        final Method[] candidateMethods = staticOnly ? clazz.getMethods() : clazz.getDeclaredMethods();
        for (final Method method : candidateMethods)
        {
            if (!method.getName().equals(methodName))
            {
                continue;
            }
            final int modifiers = method.getModifiers();
            if (!Modifier.isPublic(modifiers))
            {
                continue;
            }
            if (staticOnly && !Modifier.isStatic(modifiers))
            {
                continue;
            }
            if (method.getDeclaringClass() == Object.class)
            {
                continue;
            }
            if (!method.isAnnotationPresent(AiMethod.class))
            {
                continue;
            }

            final Class<?>[] paramTypes = method.getParameterTypes();
            final int expectedCount = paramTypes.length;

            try
            {
                final List<String> argStrings;
                if (values == null || values.isEmpty())
                {
                    if (expectedCount == 0)
                    {
                        argStrings = List.of();
                    }
                    else
                    {
                        continue;
                    }
                }
                else if (values.size() == expectedCount)
                {
                    argStrings = values;
                }
                else if (values.size() == 1)
                {
                    argStrings = parseArguments(values.get(0), expectedCount);
                    if (argStrings.size() != expectedCount)
                    {
                        continue;
                    }
                }
                else
                {
                    continue;
                }

                final Object[] convertedArgs = new Object[expectedCount];
                for (int i = 0; i < expectedCount; i++)
                {
                    convertedArgs[i] = convertValue(argStrings.get(i), paramTypes[i]);
                }

                return new ResolvedMethodCall(method, convertedArgs);
            }
            catch (final Exception e)
            {
                // Mismatch or conversion failure, try next overload
            }
        }
        return null;
    }

    /**
     * Splits arguments by comma, respecting quotes.
     *
     * @param input the input string
     * @return the list of split argument strings
     */
    private static List<String> splitArguments(final String input)
    {
        final List<String> result = new ArrayList<>();
        if (input == null || input.isEmpty())
        {
            return result;
        }
        final StringBuilder current = new StringBuilder();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        int bracketDepth = 0;
        for (int i = 0; i < input.length(); i++)
        {
            final char c = input.charAt(i);
            if (c == '\\')
            {
                if (i + 1 < input.length())
                {
                    current.append(c);
                    current.append(input.charAt(i + 1));
                    i++;
                }
                else
                {
                    current.append(c);
                }
            }
            else if (c == '"' && !inSingleQuotes)
            {
                inDoubleQuotes = !inDoubleQuotes;
                current.append(c);
            }
            else if (c == '\'' && !inDoubleQuotes)
            {
                inSingleQuotes = !inSingleQuotes;
                current.append(c);
            }
            else if (c == '[' && !inDoubleQuotes && !inSingleQuotes)
            {
                bracketDepth++;
                current.append(c);
            }
            else if (c == ']' && !inDoubleQuotes && !inSingleQuotes)
            {
                bracketDepth--;
                current.append(c);
            }
            else if (c == ',' && !inDoubleQuotes && !inSingleQuotes && bracketDepth == 0)
            {
                result.add(current.toString().trim());
                current.setLength(0);
            }
            else
            {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    /**
     * Unwraps outer quotes and unescapes nested quotes.
     *
     * @param str the string to unwrap
     * @return the unwrapped string
     */
    private static String unwrapQuotes(final String str)
    {
        if (str == null)
        {
            return null;
        }
        final String trimmed = str.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\""))
        {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (trimmed.startsWith("'") && trimmed.endsWith("'"))
        {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\'", "'").replace("\\\\", "\\");
        }
        return trimmed;
    }

    /**
     * Parses raw arguments for a method, handling JSON array unwrapping and comma splitting.
     *
     * @param rawArgs the raw arguments string
     * @param expectedParamCount the expected parameter count
     * @return list of argument strings
     */
    private static List<String> parseArguments(final String rawArgs, final int expectedParamCount)
    {
        if (rawArgs == null)
        {
            return List.of();
        }
        String trimmed = rawArgs.trim();
        if (expectedParamCount > 1 && trimmed.startsWith("[") && trimmed.endsWith("]"))
        {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        final List<String> split = splitArguments(trimmed);
        final List<String> result = new ArrayList<>();
        for (final String s : split)
        {
            result.add(unwrapQuotes(s));
        }
        return result;
    }

    /**
     * Converts a string value to the target class type.
     *
     * @param val        the string value
     * @param targetType the target class type
     * @return the converted object
     */
    private static Object convertValue(final String val, final Class<?> targetType)
    {
        if (targetType == String.class)
        {
            return val;
        }
        if (val == null)
        {
            if (targetType.isPrimitive())
            {
                if (targetType == boolean.class) return false;
                if (targetType == char.class) return '\0';
                return 0;
            }
            return null;
        }
        final String trimmed = val.trim();
        if (targetType == int.class || targetType == Integer.class)
        {
            return Integer.parseInt(trimmed);
        }
        if (targetType == long.class || targetType == Long.class)
        {
            return Long.parseLong(trimmed);
        }
        if (targetType == double.class || targetType == Double.class)
        {
            return Double.parseDouble(trimmed);
        }
        if (targetType == float.class || targetType == Float.class)
        {
            return Float.parseFloat(trimmed);
        }
        if (targetType == boolean.class || targetType == Boolean.class)
        {
            return Boolean.parseBoolean(trimmed);
        }
        if (targetType == java.math.BigDecimal.class)
        {
            return new java.math.BigDecimal(trimmed);
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + targetType.getName());
    }

    /**
     * Invokes the resolved method.
     *
     * @param call       the resolved method call
     * @param instance   the test instance (null for static)
     * @param methodName the name of the method
     */
    private static void invokeMethod(final ResolvedMethodCall call, final Object instance, final String methodName)
    {
        try
        {
            final boolean isStatic = Modifier.isStatic(call.method.getModifiers());
            final Object target = isStatic ? null : instance;
            LOG.debug("Invoking {}method {}({})", isStatic ? "static " : "", methodName, java.util.Arrays.toString(call.args));
            call.method.invoke(target, call.args);
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
     * Scans classes in package using the class loader.
     */
    private static List<Class<?>> getClassesInPackage(final String packageName)
    {
        final List<Class<?>> classes = new ArrayList<>();
        final String path = packageName.replace('.', '/');
        try
        {
            final java.util.Enumeration<java.net.URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements())
            {
                final java.net.URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file"))
                {
                    classes.addAll(findClassesInDirectory(new java.io.File(resource.toURI()), packageName));
                }
                else if (resource.getProtocol().equals("jar"))
                {
                    final java.net.JarURLConnection conn = (java.net.JarURLConnection) resource.openConnection();
                    final java.util.jar.JarFile jar = conn.getJarFile();
                    final java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements())
                    {
                        final java.util.jar.JarEntry entry = entries.nextElement();
                        final String name = entry.getName();
                        if (name.startsWith(path) && name.endsWith(".class") && !name.contains("$"))
                        {
                            final String className = name.substring(0, name.length() - 6).replace('/', '.');
                            try
                            {
                                classes.add(Class.forName(className));
                            }
                            catch (final ClassNotFoundException e)
                            {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to scan package: " + packageName, e);
        }
        return classes;
    }

    private static List<Class<?>> findClassesInDirectory(final java.io.File directory, final String packageName)
    {
        final List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists())
        {
            return classes;
        }
        final java.io.File[] files = directory.listFiles();
        if (files != null)
        {
            for (final java.io.File file : files)
            {
                if (file.isDirectory())
                {
                    classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
                }
                else if (file.getName().endsWith(".class") && !file.getName().contains("$"))
                {
                    try
                    {
                        classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
                    }
                    catch (final ClassNotFoundException e)
                    {
                        // ignore
                    }
                }
            }
        }
        return classes;
    }

    /**
     * Simple holder for a resolved method and its arguments.
     */
    private static final class ResolvedMethodCall
    {
        final Method method;
        final Object[] args;

        ResolvedMethodCall(final Method method, final Object[] args)
        {
            this.method = method;
            this.args = args;
        }
    }
}
