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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.util.AiAssertions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Unit tests verifying reflective Java tool schema generation, typed argument
 * deserialization, and method execution in {@link JavaToolFactory} and {@link JavaTool}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class JavaToolFactoryTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class SampleMathTools
    {
        @Tool(name = "calculate_total", description = "Calculates total price with tax")
        public static BigDecimal calculateTotal(
                @ToolParam(name = "basePrice", description = "Base price before tax") final BigDecimal basePrice,
                @ToolParam(name = "taxRate", description = "Tax rate percentage (e.g. 0.19)") final double taxRate,
                @ToolParam(name = "discountCode", description = "Optional promo code", required = false) final String discountCode)
        {
            final BigDecimal tax = basePrice.multiply(BigDecimal.valueOf(taxRate));
            BigDecimal total = basePrice.add(tax);
            if ("SAVE10".equalsIgnoreCase(discountCode))
            {
                total = total.multiply(BigDecimal.valueOf(0.90));
            }
            return total.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        @Tool(description = "Validates that a count is strictly greater than zero")
        public static void assertCountPositive(
                @ToolParam(name = "count", description = "Item count") final int count)
        {
            if (count <= 0)
            {
                throw new AssertionError("Count must be > 0 but was " + count);
            }
        }
    }

    public static class SampleStatefulService
    {
        private int counter = 0;

        @Tool(name = "increment_counter", description = "Increments internal counter")
        public int increment(
                @ToolParam(name = "amount", description = "Amount to add") final int amount)
        {
            this.counter += amount;
            return this.counter;
        }

        public int getCounter()
        {
            return this.counter;
        }
    }

    @Test
    public void testSchemaGenerationForStaticMethods()
    {
        final List<AiTool> tools = JavaToolFactory.createToolsFromClass(SampleMathTools.class);
        Assertions.assertEquals(2, tools.size());

        final Optional<AiTool> totalToolOpt = tools.stream()
                .filter(t -> "calculate_total".equals(t.getDefinition().name()))
                .findFirst();
        Assertions.assertTrue(totalToolOpt.isPresent());

        final ToolDefinition definition = totalToolOpt.get().getDefinition();
        Assertions.assertEquals("calculate_total", definition.name());
        Assertions.assertEquals("Calculates total price with tax", definition.description());

        final ObjectNode schema = definition.parametersSchema();
        Assertions.assertEquals("object", schema.path("type").asText());

        final JsonNode props = schema.path("properties");
        Assertions.assertEquals("number", props.path("basePrice").path("type").asText());
        Assertions.assertEquals("Base price before tax", props.path("basePrice").path("description").asText());

        Assertions.assertEquals("number", props.path("taxRate").path("type").asText());
        Assertions.assertEquals("string", props.path("discountCode").path("type").asText());

        final JsonNode req = schema.path("required");
        Assertions.assertTrue(req.isArray());
        final List<String> reqFields = new java.util.ArrayList<>();
        req.forEach(item -> reqFields.add(item.asText()));

        Assertions.assertTrue(reqFields.contains("basePrice"));
        Assertions.assertTrue(reqFields.contains("taxRate"));
        Assertions.assertFalse(reqFields.contains("discountCode"), "discountCode should be optional");
    }

    @Test
    public void testExecutionWithTypedConversionAndReturnVariable() throws Exception
    {
        final List<AiTool> tools = JavaToolFactory.createToolsFromClass(SampleMathTools.class);
        final AiTool totalTool = tools.stream()
                .filter(t -> "calculate_total".equals(t.getDefinition().name()))
                .findFirst()
                .orElseThrow();

        final ToolRegistry registry = new ToolRegistry();
        registry.register(totalTool);
        final SimpleToolContext context = new SimpleToolContext(registry);

        final ObjectNode args = MAPPER.createObjectNode();
        args.put("basePrice", 100.0);
        args.put("taxRate", 0.19);
        args.put("discountCode", "SAVE10");
        args.put("store", "finalCalculatedTotal");

        final ToolCall call = new ToolCall("call_calc_1", "calculate_total", args);
        final ToolResult result = totalTool.execute(call, context);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("107.10", result.content());

        // Verify variable bound to context
        final Optional<BigDecimal> storedVar = context.getVariable("finalCalculatedTotal", BigDecimal.class);
        Assertions.assertTrue(storedVar.isPresent());
        Assertions.assertEquals(new BigDecimal("107.10"), storedVar.get());
    }

    @Test
    public void testAssertionFailureRethrownOrCaptured()
    {
        final List<AiTool> tools = JavaToolFactory.createToolsFromClass(SampleMathTools.class);
        final AiTool assertTool = tools.stream()
                .filter(t -> "assertCountPositive".equals(t.getDefinition().name()))
                .findFirst()
                .orElseThrow();

        final ToolRegistry registry = new ToolRegistry();
        final SimpleToolContext context = new SimpleToolContext(registry);

        // Valid execution
        final ObjectNode validArgs = MAPPER.createObjectNode();
        validArgs.put("count", 5);
        final ToolCall validCall = new ToolCall("call_a1", "assertCountPositive", validArgs);
        Assertions.assertDoesNotThrow(() -> assertTool.execute(validCall, context));

        // Invalid execution throws AssertionError
        final ObjectNode invalidArgs = MAPPER.createObjectNode();
        invalidArgs.put("count", -1);
        final ToolCall invalidCall = new ToolCall("call_a2", "assertCountPositive", invalidArgs);

        final AssertionError err = Assertions.assertThrows(AssertionError.class, () -> assertTool.execute(invalidCall, context));
        Assertions.assertTrue(err.getMessage().contains("Count must be > 0"));
    }

    @Test
    public void testInstanceMethodToolExecution() throws Exception
    {
        final SampleStatefulService service = new SampleStatefulService();
        final List<AiTool> tools = JavaToolFactory.createToolsFromInstance(service);
        Assertions.assertEquals(1, tools.size());

        final AiTool incTool = tools.get(0);
        final ToolRegistry registry = new ToolRegistry();
        final SimpleToolContext context = new SimpleToolContext(registry);

        final ObjectNode args1 = MAPPER.createObjectNode();
        args1.put("amount", 7);
        incTool.execute(new ToolCall("call_i1", "increment_counter", args1), context);

        final ObjectNode args2 = MAPPER.createObjectNode();
        args2.put("amount", 3);
        final ToolResult result2 = incTool.execute(new ToolCall("call_i2", "increment_counter", args2), context);

        Assertions.assertEquals("10", result2.content());
        Assertions.assertEquals(10, service.getCounter());
    }

    @Test
    public void testAiAssertionsClassScan()
    {
        final List<AiTool> tools = JavaToolFactory.createToolsFromClass(AiAssertions.class);
        Assertions.assertTrue(tools.size() >= 10, "Expected at least 10 assertion tools from AiAssertions");

        final Optional<AiTool> priceTool = tools.stream()
                .filter(t -> "assertPriceGreaterThanZero".equals(t.getDefinition().name()))
                .findFirst();

        Assertions.assertTrue(priceTool.isPresent());
        Assertions.assertTrue(priceTool.get().getDefinition().description().contains("greater than zero"));
    }
}
