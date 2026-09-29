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
package org.neodymium.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.executor.selenide.plugins.AiMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Factory for creating {@link AiTool} instances by reflectively inspecting Java methods
 * annotated with {@link Tool} or {@link AiToolMethod}. Automatically generates OpenAPI/JSON Schema
 * definitions and handles typed argument conversion.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class JavaToolFactory
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JavaToolFactory()
    {
        // Static factory
    }

    /**
     * Inspects public static methods on the given class annotated with {@link Tool}
     * or {@link AiToolMethod} and creates {@link AiTool} instances.
     *
     * @param clazz class to inspect
     * @return list of created AiTool instances
     */
    public static List<AiTool> createToolsFromClass(final Class<?> clazz)
    {
        if (clazz == null)
        {
            throw new IllegalArgumentException("Class must not be null.");
        }

        final List<AiTool> tools = new ArrayList<>();
        for (final Method method : clazz.getMethods())
        {
            if (Modifier.isPublic(method.getModifiers()) && isToolMethod(method))
            {
                tools.add(createJavaTool(method, null));
            }
        }
        return Collections.unmodifiableList(tools);
    }

    /**
     * Inspects public instance methods on the given object instance annotated with {@link Tool}
     * or {@link AiToolMethod} and creates {@link AiTool} instances bound to the instance.
     *
     * @param instance instance to inspect
     * @return list of created AiTool instances
     */
    public static List<AiTool> createToolsFromInstance(final Object instance)
    {
        if (instance == null)
        {
            throw new IllegalArgumentException("Instance must not be null.");
        }

        final List<AiTool> tools = new ArrayList<>();
        for (final Method method : instance.getClass().getMethods())
        {
            if (Modifier.isPublic(method.getModifiers()) && isToolMethod(method))
            {
                tools.add(createJavaTool(method, instance));
            }
        }
        return Collections.unmodifiableList(tools);
    }

    private static boolean isToolMethod(final Method method)
    {
        return method.isAnnotationPresent(Tool.class)
                || method.isAnnotationPresent(AiToolMethod.class)
                || method.isAnnotationPresent(AiMethod.class);
    }

    private static JavaTool createJavaTool(final Method method, final Object targetInstance)
    {
        final String toolName = resolveToolName(method);
        final String description = resolveToolDescription(method);

        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode propertiesNode = schema.putObject("properties");
        final ArrayNode requiredNode = schema.putArray("required");

        final Parameter[] parameters = method.getParameters();
        final List<String> parameterNames = new ArrayList<>(parameters.length);

        for (int i = 0; i < parameters.length; i++)
        {
            final Parameter param = parameters[i];
            final ToolParam toolParam = param.getAnnotation(ToolParam.class);

            final String paramName = (toolParam != null && !toolParam.name().isBlank())
                    ? toolParam.name()
                    : param.getName();
            parameterNames.add(paramName);

            final ObjectNode propObj = propertiesNode.putObject(paramName);
            propObj.put("type", mapJavaTypeToJsonType(param.getType()));

            if (toolParam != null && !toolParam.description().isBlank())
            {
                propObj.put("description", toolParam.description());
            }

            final boolean isRequired = toolParam == null || toolParam.required();
            if (isRequired)
            {
                requiredNode.add(paramName);
            }
        }

        // Add optional 'store' argument in schema if method returns a value
        if (method.getReturnType() != void.class)
        {
            final ObjectNode storeProp = propertiesNode.putObject("store");
            storeProp.put("type", "string");
            storeProp.put("description", "Optional session variable key to store the return value of this tool execution");
        }

        final ToolDefinition definition = new ToolDefinition(toolName, description, schema);
        return new JavaTool(method, targetInstance, definition, parameterNames, MAPPER);
    }

    private static String resolveToolName(final Method method)
    {
        final Tool tool = method.getAnnotation(Tool.class);
        if (tool != null && !tool.name().isBlank())
        {
            return tool.name();
        }
        final AiToolMethod aiToolMethod = method.getAnnotation(AiToolMethod.class);
        if (aiToolMethod != null && !aiToolMethod.name().isBlank())
        {
            return aiToolMethod.name();
        }
        return method.getName();
    }

    private static String resolveToolDescription(final Method method)
    {
        final Tool tool = method.getAnnotation(Tool.class);
        if (tool != null && !tool.description().isBlank())
        {
            return tool.description();
        }
        final AiToolMethod aiToolMethod = method.getAnnotation(AiToolMethod.class);
        if (aiToolMethod != null && !aiToolMethod.description().isBlank())
        {
            return aiToolMethod.description();
        }
        final AiMethod legacyAiMethod = method.getAnnotation(AiMethod.class);
        if (legacyAiMethod != null && !legacyAiMethod.value().isBlank())
        {
            return legacyAiMethod.value();
        }
        return "Executes Java method " + method.getName();
    }

    /**
     * Maps a Java type to an OpenAPI/JSON Schema data type.
     *
     * @param type the Java class
     * @return JSON Schema type string ("string", "integer", "number", "boolean", "array", "object")
     */
    public static String mapJavaTypeToJsonType(final Class<?> type)
    {
        if (type == String.class || type == char.class || type == Character.class || CharSequence.class.isAssignableFrom(type))
        {
            return "string";
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == short.class || type == Short.class || type == byte.class || type == Byte.class
                || type == BigInteger.class)
        {
            return "integer";
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class
                || type == BigDecimal.class || Number.class.isAssignableFrom(type))
        {
            return "number";
        }
        if (type == boolean.class || type == Boolean.class)
        {
            return "boolean";
        }
        if (type.isArray() || Collection.class.isAssignableFrom(type))
        {
            return "array";
        }
        return "object";
    }
}
