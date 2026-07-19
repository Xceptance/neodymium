/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neodymium.ai.executor.selenide.plugins;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action plugin that invokes a Java method by name via reflection.
 * Resolution is performed against the Junit test instance or configured helper classes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class JavaMethodAction implements BrowserActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(JavaMethodAction.class);

    private static final Set<Class<?>> staticConfigurationClasses = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> staticConfigurationMethods = new ConcurrentHashMap<>();
    private static volatile boolean configurationScanned = false;
    private static List<String> lastScannedClasses = null;
    private static List<String> lastScannedPackages = null;

    private final ExecutionContext context;

    /**
     * Constructs a JavaMethodAction with the given execution context.
     *
     * @param context the execution context
     */
    public JavaMethodAction(final ExecutionContext context)
    {
        this.context = context;
    }

    @Override
    public void execute(final Action action) throws Exception
    {
        final Object testInstance = this.context != null ? this.context.getTransientData().get("junit.testInstance") : null;

        final String target = action.getTarget();
        if (target == null || target.isBlank())
        {
            throw new IllegalArgumentException("JAVA_METHOD action requires a 'target' containing the method name");
        }

        final int lastDot = target.lastIndexOf('.');
        if (lastDot >= 0)
        {
            throw new IllegalArgumentException("JAVA_METHOD target must be a simple method name (no dots), got: " + target);
        }

        final String methodName = target;
        final List<String> values = action.getValues();

        LOG.debug("JAVA_METHOD: method='{}', values='{}'", methodName, values);

        scanConfigurationIfNeeded();

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

        // Stage 2: Scan registered/cached static utility classes and packages
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
        throw new IllegalArgumentException(
                String.format("JAVA_METHOD: no public method '%s' annotated with @AiMethod found on class '%s', or configured utility classes/packages",
                        methodName, testInstance != null ? testInstance.getClass().getSimpleName() : "null"));
    }

    private List<String> getResolvedClasses()
    {
        final AiConfiguration config = new AiConfiguration();
        final String classesStr = config.getProperty("neodymium.ai.agent.methods.classes", "org.neodymium.ai.util.AiAssertions");
        final List<String> classes = new ArrayList<>();
        if (classesStr != null && !classesStr.isBlank())
        {
            for (final String s : classesStr.split(","))
            {
                if (!s.isBlank())
                {
                    classes.add(s.trim());
                }
            }
        }
        return classes;
    }

    private List<String> getResolvedPackages()
    {
        final AiConfiguration config = new AiConfiguration();
        final String packagesStr = config.getProperty("neodymium.ai.agent.methods.packages", "");
        final List<String> packages = new ArrayList<>();
        if (packagesStr != null && !packagesStr.isBlank())
        {
            for (final String s : packagesStr.split(","))
            {
                if (!s.isBlank())
                {
                    packages.add(s.trim());
                }
            }
        }
        return packages;
    }

    private void scanConfigurationIfNeeded()
    {
        final List<String> classes = getResolvedClasses();
        final List<String> packages = getResolvedPackages();

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

    private static String formatMethodSignature(final Method method)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        final Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++)
        {
            sb.append(paramTypes[i].getSimpleName());
            if (i < paramTypes.length - 1)
            {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

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

    private static void invokeMethod(final ResolvedMethodCall call, final Object instance, final String methodName) throws Exception
    {
        final boolean isStatic = Modifier.isStatic(call.method.getModifiers());
        final Object target = isStatic ? null : instance;
        LOG.debug("Invoking {}method {}({})", isStatic ? "static " : "", methodName, java.util.Arrays.toString(call.args));
        call.method.invoke(target, call.args);
    }

    private static List<Class<?>> getClassesInPackage(final String packageName)
    {
        final List<Class<?>> classes = new ArrayList<>();
        final String path = packageName.replace('.', '/');
        try
        {
            final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements())
            {
                final URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file"))
                {
                    classes.addAll(findClassesInDirectory(new File(resource.toURI()), packageName));
                }
                else if (resource.getProtocol().equals("jar"))
                {
                    final JarURLConnection conn = (JarURLConnection) resource.openConnection();
                    final JarFile jar = conn.getJarFile();
                    final Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements())
                    {
                        final JarEntry entry = entries.nextElement();
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

    private static List<Class<?>> findClassesInDirectory(final File directory, final String packageName)
    {
        final List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists())
        {
            return classes;
        }
        final File[] files = directory.listFiles();
        if (files != null)
        {
            for (final File file : files)
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
