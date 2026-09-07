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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

/**
 * An {@link AiTool} implementation backed by an annotated Java method, supporting
 * automatic Jackson-based argument conversion, typed execution, and return value variable binding.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class JavaTool implements AiTool
{
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private final Method method;
    private final Object targetInstance;
    private final ToolDefinition definition;
    private final List<String> parameterNames;
    private final ObjectMapper mapper;

    /**
     * Constructs a JavaTool instance with the specified method and metadata.
     *
     * @param method target method to invoke
     * @param targetInstance instance on which to invoke the method, or null for static methods
     * @param definition tool schema definition
     * @param parameterNames ordered list of parameter names
     */
    public JavaTool(
            final Method method,
            final Object targetInstance,
            final ToolDefinition definition,
            final List<String> parameterNames)
    {
        this(method, targetInstance, definition, parameterNames, DEFAULT_MAPPER);
    }

    /**
     * Constructs a JavaTool instance with custom ObjectMapper.
     *
     * @param method target method to invoke
     * @param targetInstance instance on which to invoke the method, or null for static methods
     * @param definition tool schema definition
     * @param parameterNames ordered list of parameter names
     * @param mapper Jackson ObjectMapper for typed argument conversion
     */
    public JavaTool(
            final Method method,
            final Object targetInstance,
            final ToolDefinition definition,
            final List<String> parameterNames,
            final ObjectMapper mapper)
    {
        if (method == null)
        {
            throw new IllegalArgumentException("Method must not be null.");
        }
        if (definition == null)
        {
            throw new IllegalArgumentException("ToolDefinition must not be null.");
        }
        if (parameterNames == null)
        {
            throw new IllegalArgumentException("Parameter names must not be null.");
        }
        if (mapper == null)
        {
            throw new IllegalArgumentException("ObjectMapper must not be null.");
        }
        this.method = method;
        this.targetInstance = targetInstance;
        this.definition = definition;
        this.parameterNames = List.copyOf(parameterNames);
        this.mapper = mapper;
    }

    @Override
    public ToolDefinition getDefinition()
    {
        return this.definition;
    }

    @Override
    public ToolResult execute(final ToolCall call, final ToolContext context) throws Exception
    {
        final Parameter[] parameters = this.method.getParameters();
        final Object[] args = new Object[parameters.length];
        final JsonNode callArgs = call.arguments();

        for (int i = 0; i < parameters.length; i++)
        {
            final Parameter param = parameters[i];
            final String paramName = this.parameterNames.get(i);
            final Class<?> paramType = param.getType();

            if (callArgs != null && callArgs.has(paramName) && !callArgs.get(paramName).isNull())
            {
                final JsonNode valueNode = callArgs.get(paramName);
                args[i] = this.mapper.convertValue(valueNode, paramType);
            }
            else
            {
                if (paramType == Optional.class)
                {
                    args[i] = Optional.empty();
                }
                else if (paramType.isPrimitive())
                {
                    args[i] = resolvePrimitiveDefault(paramType);
                }
                else
                {
                    args[i] = null;
                }
            }
        }

        this.method.setAccessible(true);
        final Object resultValue;
        try
        {
            resultValue = this.method.invoke(this.targetInstance, args);
        }
        catch (final InvocationTargetException ite)
        {
            final Throwable cause = ite.getCause();
            if (cause instanceof AssertionError ae)
            {
                throw ae;
            }
            if (cause instanceof Exception e)
            {
                return ToolResult.error(call.callId(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
            throw new RuntimeException("Error executing method " + this.method.getName(), cause);
        }

        final ToolResult.Builder builder = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS);

        if (this.method.getReturnType() == void.class)
        {
            builder.withContent("Executed successfully.");
        }
        else
        {
            final String contentString = resultValue != null ? resultValue.toString() : "null";
            builder.withContent(contentString);

            // Check if caller requested storing return value into a session variable
            if (callArgs != null && callArgs.hasNonNull("store"))
            {
                final String varName = callArgs.path("store").asText();
                if (!varName.isBlank() && context != null)
                {
                    context.setVariable(varName, resultValue);
                    builder.withVariable(varName, resultValue);
                }
            }
        }

        return builder.build();
    }

    private static Object resolvePrimitiveDefault(final Class<?> primitiveType)
    {
        if (primitiveType == boolean.class)
        {
            return false;
        }
        if (primitiveType == byte.class)
        {
            return (byte) 0;
        }
        if (primitiveType == short.class)
        {
            return (short) 0;
        }
        if (primitiveType == int.class)
        {
            return 0;
        }
        if (primitiveType == long.class)
        {
            return 0L;
        }
        if (primitiveType == float.class)
        {
            return 0.0f;
        }
        if (primitiveType == double.class)
        {
            return 0.0d;
        }
        if (primitiveType == char.class)
        {
            return '\0';
        }
        return null;
    }
}
