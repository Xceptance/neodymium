/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.aura.mcp.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ClassUtils;

/**
 * Shared reflection and Spring bean discovery helper for {@link McpTool}-annotated services.
 * Consolidates tool metadata, parameter naming, and requiredness rules across chat tool bridging
 * and the Settings MCP overview.
 *
 * @author AI-generated: GPT-5.4
 * @author Xceptance GmbH 2026
 */
public final class McpToolCatalogHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(McpToolCatalogHelper.class);
    private static final String SCOPED_TARGET_PREFIX = "scopedTarget.";

    private McpToolCatalogHelper()
    {
    }

    /**
     * Canonical metadata describing a single {@link McpTool} method parameter.
     *
     * @param name canonical JSON parameter name ({@link JsonProperty} value or Java parameter name)
     * @param rawName raw Java parameter name
     * @param description parameter description from {@link McpToolParam} or {@link ToolParam}
     * @param explicitRequired explicit required flag when annotated, or {@code null} when unspecified
     * @param nullable whether a {@code Nullable} annotation marks the parameter optional
     * @param annotatedForDisplay whether {@link McpToolParam} or {@link ToolParam} is present
     */
    public record McpToolParamDescriptor(
            String name,
            String rawName,
            String description,
            Boolean explicitRequired,
            boolean nullable,
            boolean annotatedForDisplay)
    {
        /**
         * Resolves final JSON Schema requiredness using nullable and explicit annotation overrides first,
         * falling back to pre-existing schema required entries when unannotated.
         *
         * @param originalRequired parameter names originally listed in schema {@code required}
         * @return {@code true} if the parameter must be required
         */
        public boolean isRequired(final Set<String> originalRequired)
        {
            if (nullable)
            {
                return false;
            }
            if (explicitRequired != null)
            {
                return explicitRequired;
            }
            return originalRequired != null
                    && (originalRequired.contains(name) || originalRequired.contains(rawName));
        }

        /**
         * Formats this parameter for human-readable UI display.
         *
         * @return formatted {@code name (description)} or {@code name}
         */
        public String formatDisplay()
        {
            if (description != null && !description.isBlank())
            {
                return name + " (" + description + ")";
            }
            return name;
        }
    }

    /**
     * Canonical metadata describing a single discovered {@link McpTool} method on a target bean.
     *
     * @param name resolved MCP tool name
     * @param description tool description
     * @param method target Java method declared on the resolved service class
     * @param bean target Spring bean instance
     * @param parameters ordered parameter metadata (excluding {@link ToolContext})
     */
    public record McpToolDescriptor(
            String name,
            String description,
            Method method,
            Object bean,
            List<McpToolParamDescriptor> parameters)
    {
        /**
         * Formats all annotated parameters into a single comma-separated summary for UI rendering.
         *
         * @return parameter summary, or {@code "None"} when no annotated parameters exist
         */
        public String formatParametersSummary()
        {
            final StringBuilder summary = new StringBuilder();
            for (final McpToolParamDescriptor param : parameters)
            {
                if (!param.annotatedForDisplay())
                {
                    continue;
                }
                if (!summary.isEmpty())
                {
                    summary.append(", ");
                }
                summary.append(param.formatDisplay());
            }
            return summary.isEmpty() ? "None" : summary.toString();
        }
    }

    /**
     * Discovers all {@link McpTool}-annotated methods from non-abstract beans in the provided Spring bean factory.
     *
     * @param beanFactory Spring bean factory to inspect
     * @return sorted list of discovered tool descriptors
     */
    public static List<McpToolDescriptor> resolveFromBeanFactory(final ListableBeanFactory beanFactory)
    {
        if (beanFactory == null)
        {
            return List.of();
        }

        final List<Object> toolBeans = new ArrayList<>();
        for (final String beanName : beanFactory.getBeanDefinitionNames())
        {
            if (beanName.startsWith(SCOPED_TARGET_PREFIX) || isAbstractBeanDefinition(beanFactory, beanName))
            {
                continue;
            }
            final Class<?> beanType = beanFactory.getType(beanName, false);
            if (beanType == null || resolveAnnotatedClass(beanType) == null)
            {
                continue;
            }
            try
            {
                toolBeans.add(beanFactory.getBean(beanName));
            }
            catch (final BeansException e)
            {
                LOGGER.warn("Skipping MCP tool bean '{}': {}", beanName, e.getMessage());
            }
        }
        return resolveFromBeans(toolBeans);
    }

    /**
     * Extracts {@link McpToolDescriptor} entries from the supplied service instances.
     * Supports plain classes, Spring CGLIB proxies, and Mockito subclasses cleanly.
     *
     * @param beans candidate service beans containing {@link McpTool} methods
     * @return sorted, deduplicated list of tool descriptors
     */
    public static List<McpToolDescriptor> resolveFromBeans(final Object... beans)
    {
        if (beans == null || beans.length == 0)
        {
            return List.of();
        }
        return resolveFromBeans(Arrays.asList(beans));
    }

    /**
     * Extracts {@link McpToolDescriptor} entries from the supplied collection of service instances.
     * Supports plain classes, Spring CGLIB proxies, and Mockito subclasses cleanly.
     *
     * @param beans candidate service beans containing {@link McpTool} methods
     * @return sorted, deduplicated list of tool descriptors
     */
    public static List<McpToolDescriptor> resolveFromBeans(final Collection<?> beans)
    {
        if (beans == null || beans.isEmpty())
        {
            return List.of();
        }

        final Set<Object> visitedBeans = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<String> registeredToolNames = new HashSet<>();
        final List<McpToolDescriptor> descriptors = new ArrayList<>();

        for (final Object bean : beans)
        {
            if (bean == null || !visitedBeans.add(bean))
            {
                continue;
            }
            final Class<?> targetClass = resolveAnnotatedClass(bean.getClass());
            if (targetClass == null)
            {
                continue;
            }

            final List<Method> methods = collectCandidateMethods(targetClass);
            methods.sort(Comparator.comparing(Method::getName));
            for (final Method method : methods)
            {
                final McpTool annotation = method.getAnnotation(McpTool.class);
                if (annotation == null)
                {
                    continue;
                }
                final String toolName = (annotation.name() != null && !annotation.name().isBlank())
                        ? annotation.name()
                        : method.getName();
                if (!registeredToolNames.add(toolName))
                {
                    continue;
                }
                final String description = annotation.description() != null ? annotation.description() : "";
                descriptors.add(new McpToolDescriptor(
                        toolName,
                        description,
                        method,
                        bean,
                        extractParameters(method)));
            }
        }

        descriptors.sort(Comparator.comparing(McpToolDescriptor::name));
        return List.copyOf(descriptors);
    }

    /**
     * Extracts canonical parameter descriptors for an {@link McpTool} method.
     *
     * @param method reflected tool method
     * @return immutable list of non-{@link ToolContext} parameter descriptors in declaration order
     */
    public static List<McpToolParamDescriptor> extractParameters(final Method method)
    {
        if (method == null)
        {
            return List.of();
        }

        final List<McpToolParamDescriptor> params = new ArrayList<>();
        for (final Parameter param : method.getParameters())
        {
            if (ToolContext.class.isAssignableFrom(param.getType()))
            {
                continue;
            }

            final JsonProperty jsonProperty = param.getAnnotation(JsonProperty.class);
            final String rawName = param.getName();
            final String paramName = (jsonProperty != null && jsonProperty.value() != null
                    && !jsonProperty.value().isBlank())
                            ? jsonProperty.value()
                            : rawName;

            final McpToolParam mcpParam = param.getAnnotation(McpToolParam.class);
            final ToolParam toolParam = param.getAnnotation(ToolParam.class);
            final boolean hasNullable = Arrays.stream(param.getAnnotations())
                    .anyMatch(ann -> "Nullable".equals(ann.annotationType().getSimpleName()));

            final String description = resolveParamDescription(mcpParam, toolParam);
            final Boolean explicitRequired;
            if (mcpParam != null)
            {
                explicitRequired = mcpParam.required();
            }
            else if (toolParam != null)
            {
                explicitRequired = toolParam.required();
            }
            else
            {
                explicitRequired = null;
            }

            params.add(new McpToolParamDescriptor(
                    paramName,
                    rawName,
                    description,
                    explicitRequired,
                    hasNullable,
                    mcpParam != null || toolParam != null));
        }
        return List.copyOf(params);
    }

    private static String resolveParamDescription(final McpToolParam mcpParam, final ToolParam toolParam)
    {
        if (mcpParam != null && mcpParam.description() != null && !mcpParam.description().isBlank())
        {
            return mcpParam.description();
        }
        if (toolParam != null && toolParam.description() != null && !toolParam.description().isBlank())
        {
            return toolParam.description();
        }
        return "";
    }

    private static boolean isAbstractBeanDefinition(final ListableBeanFactory beanFactory, final String beanName)
    {
        if (beanFactory instanceof ConfigurableListableBeanFactory configurableFactory
                && configurableFactory.containsBeanDefinition(beanName))
        {
            return configurableFactory.getBeanDefinition(beanName).isAbstract();
        }
        return false;
    }

    private static Class<?> resolveAnnotatedClass(final Class<?> candidateClass)
    {
        Class<?> current = ClassUtils.getUserClass(candidateClass);
        while (current != null && current.getName().contains("$MockitoMock$"))
        {
            current = current.getSuperclass();
        }
        if (current == null || current == Object.class)
        {
            return null;
        }
        return hasDeclaredMcpToolMethods(current) ? current : null;
    }

    private static boolean hasDeclaredMcpToolMethods(final Class<?> type)
    {
        for (final Method method : collectCandidateMethods(type))
        {
            if (method.isAnnotationPresent(McpTool.class))
            {
                return true;
            }
        }
        return false;
    }

    private static List<Method> collectCandidateMethods(final Class<?> targetClass)
    {
        final List<Method> collected = new ArrayList<>();
        Class<?> current = targetClass;
        while (current != null && current != Object.class)
        {
            addNonSyntheticDeclaredMethods(current, collected);
            current = current.getSuperclass();
        }
        for (final Class<?> iface : ClassUtils.getAllInterfacesForClassAsSet(targetClass))
        {
            addNonSyntheticDeclaredMethods(iface, collected);
        }
        return collected;
    }

    private static void addNonSyntheticDeclaredMethods(final Class<?> type, final List<Method> target)
    {
        for (final Method method : type.getDeclaredMethods())
        {
            if (!method.isBridge() && !method.isSynthetic())
            {
                target.add(method);
            }
        }
    }
}
