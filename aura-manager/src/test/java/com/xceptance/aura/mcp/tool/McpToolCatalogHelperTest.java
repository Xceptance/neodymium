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
import com.xceptance.aura.mcp.tool.McpToolCatalogHelper.McpToolDescriptor;
import com.xceptance.aura.mcp.tool.McpToolCatalogHelper.McpToolParamDescriptor;
import jakarta.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Unit tests for {@link McpToolCatalogHelper}.
 *
 * @author AI-generated: GPT-5.4
 * @author Xceptance GmbH 2026
 */
public final class McpToolCatalogHelperTest
{
    @Test
    public void testResolveFromBeansSupportsPlainMockitoMockAndDeduplication()
    {
        final SamplePrimaryTools primaryBean = new SamplePrimaryTools();
        final SampleSecondaryTools mockedSecondary = Mockito.mock(SampleSecondaryTools.class);

        final List<McpToolDescriptor> descriptors = McpToolCatalogHelper.resolveFromBeans(
                Arrays.asList(primaryBean, null, mockedSecondary, primaryBean));

        Assertions.assertEquals(3, descriptors.size());
        Assertions.assertEquals(
                List.of("alpha_tool", "fallbackNameMethod", "secondary_tool"),
                descriptors.stream().map(McpToolDescriptor::name).toList());
        Assertions.assertSame(primaryBean, descriptors.get(0).bean());
        Assertions.assertSame(mockedSecondary, descriptors.get(2).bean());
        Assertions.assertEquals("None", descriptors.get(1).formatParametersSummary());
        Assertions.assertEquals(
                "renamed_target (Target path), optional_note (Optional note)",
                descriptors.get(0).formatParametersSummary());
    }

    @Test
    public void testExtractParametersHonorsJsonPropertyNullableAndToolContextExclusion() throws Exception
    {
        final Method method = SamplePrimaryTools.class.getDeclaredMethod(
                "alpha", String.class, String.class, String.class, ToolContext.class);

        final List<McpToolParamDescriptor> params = McpToolCatalogHelper.extractParameters(method);

        Assertions.assertEquals(3, params.size());

        final McpToolParamDescriptor first = params.get(0);
        Assertions.assertEquals("renamed_target", first.name());
        Assertions.assertEquals("Target path", first.description());
        Assertions.assertTrue(first.isRequired(Set.of()));

        final McpToolParamDescriptor second = params.get(1);
        Assertions.assertEquals("optional_note", second.name());
        Assertions.assertFalse(second.isRequired(Set.of("optional_note")));

        final McpToolParamDescriptor third = params.get(2);
        Assertions.assertEquals("nullableFlag", third.name());
        Assertions.assertFalse(third.isRequired(Set.of("nullableFlag")));
    }

    @Test
    public void testResolveFromBeanFactoryDiscoversAllAnnotatedBeansAndSkipsUnrelatedOrScopedTargets()
    {
        final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition("primaryTools", new RootBeanDefinition(SamplePrimaryTools.class));
        beanFactory.registerBeanDefinition("secondaryTools", new RootBeanDefinition(SampleSecondaryTools.class));
        beanFactory.registerBeanDefinition("scopedTarget.secondaryTools", new RootBeanDefinition(SampleSecondaryTools.class));
        beanFactory.registerBeanDefinition("plainHelper", new RootBeanDefinition(PlainHelperBean.class));

        final List<McpToolDescriptor> descriptors = McpToolCatalogHelper.resolveFromBeanFactory(beanFactory);

        Assertions.assertEquals(3, descriptors.size());
        Assertions.assertEquals(
                List.of("alpha_tool", "fallbackNameMethod", "secondary_tool"),
                descriptors.stream().map(McpToolDescriptor::name).toList());
    }

    static class SamplePrimaryTools
    {
        @McpTool(name = "alpha_tool", description = "Alpha tool description")
        public Map<String, Object> alpha(
                @JsonProperty("renamed_target") @McpToolParam(required = true, description = "Target path") final String target,
                @JsonProperty("optional_note") @McpToolParam(required = false, description = "Optional note") final String note,
                @Nullable final String nullableFlag,
                final ToolContext toolContext)
        {
            return Map.of();
        }

        @McpTool(description = "Uses method name fallback")
        public Map<String, Object> fallbackNameMethod()
        {
            return Map.of();
        }
    }

    static class SampleSecondaryTools
    {
        @McpTool(name = "secondary_tool", description = "Secondary tool")
        public Map<String, Object> secondary(
                @McpToolParam(required = true, description = "Input item") final String item)
        {
            return Map.of("item", item);
        }
    }

    static class PlainHelperBean
    {
        public String helper()
        {
            return "ignored";
        }
    }
}
