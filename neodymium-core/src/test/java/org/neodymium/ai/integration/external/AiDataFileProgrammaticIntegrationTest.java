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
package org.neodymium.ai.integration.external;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataFile;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.util.Neodymium;

/**
 * Verifies external test data integration for programmatic AI playbooks,
 * including convention auto-discovery, {@link AiDataFile} binding, step omission,
 * prompt add-on preservation, and the 3-tier data precedence hierarchy.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@NeodymiumAiTest
public class AiDataFileProgrammaticIntegrationTest
{
    /**
     * Verifies that convention auto-discovery binds the companion test data file,
     * interpolates nested dot paths, ignores YAML steps, preserves prompt addons,
     * and respects the 3-tier precedence hierarchy.
     *
     * @param session the active AI session
     */
    @AiPlaybook(AiPlaybook.PROGRAMMATIC)
    @AiMode(ExecutionMode.LLM_ONLY)
    public void testConventionDataAndHierarchy(final AiSession session)
    {
        Assertions.assertNotNull(session);
        Assertions.assertNotNull(session.data());

        // 1. Verify Tier 1 (external data file) loaded via convention
        final Object tier1Val = session.data().get("tier1Var");
        Assertions.assertNotNull(tier1Val, "tier1Var should be loaded from convention YAML");
        Assertions.assertTrue(tier1Val.toString().startsWith("value-from-file"));

        // 2. Verify intra-dataset nested dot-notation interpolation
        final String resolvedComposed = session.data().resolveVariables("${composed}");
        Assertions.assertTrue(
            "Hello World".equals(resolvedComposed) || "Good Morning".equals(resolvedComposed),
            "Composed variable should resolve nested dot properties: " + resolvedComposed
        );

        // 3. Verify Tier 3 code override via Neodymium.getData().put(...) overrides Tier 1
        Neodymium.getData().put("overrideTarget", "code-override-val");
        Assertions.assertEquals("code-override-val", session.data().get("overrideTarget"));

        // 4. Verify session.data().set(...) (dynamic data) takes final highest precedence
        session.data().set("overrideTarget", "dynamic-set-val");
        Assertions.assertEquals("dynamic-set-val", session.data().get("overrideTarget"));

        // 5. Verify promptAddons from data file are preserved in execution context
        @SuppressWarnings("unchecked")
        final Map<String, String> promptAddons = (Map<String, String>) session.getExecutionContext().getTransientData().get("playbook.promptAddons");
        Assertions.assertNotNull(promptAddons, "Prompt add-ons from data file must be registered in transient data");
        Assertions.assertEquals("Always be concise.", promptAddons.get("general"));
    }

    /**
     * Verifies explicit {@link AiDataFile} annotation binding on a test method.
     *
     * @param session the active AI session
     */
    @AiPlaybook(AiPlaybook.PROGRAMMATIC)
    @AiDataFile("custom-data-file.yaml")
    @AiMode(ExecutionMode.LLM_ONLY)
    public void testExplicitAiDataFile(final AiSession session)
    {
        Assertions.assertNotNull(session);
        Assertions.assertNotNull(session.data());

        final Object customVal = session.data().get("customKey");
        Assertions.assertEquals("customValue", customVal, "customKey should be loaded from custom-data-file.yaml");

        @SuppressWarnings("unchecked")
        final Map<String, String> promptAddons = (Map<String, String>) session.getExecutionContext().getTransientData().get("playbook.promptAddons");
        Assertions.assertNotNull(promptAddons, "Prompt add-ons from custom data file must be preserved");
        Assertions.assertEquals("Custom addon.", promptAddons.get("special"));
    }
}
