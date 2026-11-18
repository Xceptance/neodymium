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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.config.AiConfiguration;

/**
 * Tests verifying that configured custom utility classes with {@link Tool} annotations
 * are automatically discovered and registered into {@link ToolRegistry}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class CustomToolRegistryConfigurationTest
{
    private static final String PROPERTY_KEY = "neodymium.ai.agent.methods.classes";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private String originalPropertyValue;

    public static class SampleMockUploadHelper
    {
        private static String lastUploadedPath;

        @Tool(description = "Uploads a photo file to gallery")
        public static void uploadImage(final String imagePath)
        {
            lastUploadedPath = imagePath;
        }

        public static String getLastUploadedPath()
        {
            return lastUploadedPath;
        }

        public static void reset()
        {
            lastUploadedPath = null;
        }
    }

    @BeforeEach
    public void setUp()
    {
        originalPropertyValue = System.getProperty(PROPERTY_KEY);
        SampleMockUploadHelper.reset();
    }

    @AfterEach
    public void tearDown()
    {
        if (originalPropertyValue != null)
        {
            System.setProperty(PROPERTY_KEY, originalPropertyValue);
        }
        else
        {
            System.clearProperty(PROPERTY_KEY);
        }
        AiConfiguration.resetInstance();
        SampleMockUploadHelper.reset();
    }

    @Test
    public void testCustomToolsAutoRegisteredFromConfiguration() throws Exception
    {
        System.setProperty(PROPERTY_KEY, SampleMockUploadHelper.class.getName());
        AiConfiguration.resetInstance();

        final ToolRegistry registry = new ToolRegistry();
        ToolRegistry.registerConfiguredCustomTools(registry);

        Assertions.assertTrue(registry.hasTool("uploadImage"), "Expected tool 'uploadImage' to be registered");
        Assertions.assertTrue(registry.hasTool("upload_image"), "Expected snake_case lookup 'upload_image' to resolve");

        final Optional<AiTool> toolOpt = registry.getTool("upload_image");
        Assertions.assertTrue(toolOpt.isPresent());

        final AiTool tool = toolOpt.get();
        final ToolContext context = new SimpleToolContext(registry);

        // Execute using snake_case parameter or single-parameter fallback
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("imagePath", "src/test/resources/sample.png");

        final ToolCall call = new ToolCall("call_1", "upload_image", args);
        final ToolResult result = tool.execute(call, context);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("src/test/resources/sample.png", SampleMockUploadHelper.getLastUploadedPath());
    }

    @Test
    public void testResilientSingleArgumentParameterResolution() throws Exception
    {
        System.setProperty(PROPERTY_KEY, SampleMockUploadHelper.class.getName());
        AiConfiguration.resetInstance();

        final ToolRegistry registry = new ToolRegistry();
        ToolRegistry.registerConfiguredCustomTools(registry);

        final AiTool tool = registry.getTool("uploadImage").orElseThrow();
        final ToolContext context = new SimpleToolContext(registry);

        // Agent passes {"filePath": "..."} instead of exact {"imagePath": "..."}
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("filePath", "src/test/resources/other.png");

        final ToolCall call = new ToolCall("call_2", "uploadImage", args);
        final ToolResult result = tool.execute(call, context);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("src/test/resources/other.png", SampleMockUploadHelper.getLastUploadedPath());
    }
}
