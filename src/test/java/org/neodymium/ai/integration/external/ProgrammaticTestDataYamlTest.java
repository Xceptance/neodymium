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
package org.neodymium.ai.integration.external;

import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.common.browser.Browser;

/**
 * External integration test demonstrating intra-dataset variable interpolation
 * and nested dot-path resolution in a programmatic YAML playbook against Wikipedia.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000_headless")
@NeodymiumAiTest
public class ProgrammaticTestDataYamlTest
{
    /**
     * Executes the Wikipedia search workflow using programmatic YAML playbook and test data substitution with test data inside a .yaml file.
     *
     * @param session the Neodymium AI session instance
     * @throws Exception if playbook execution fails
     */
    @AiPlaybook(AiPlaybook.PROGRAMMATIC)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void test(final AiSession session) throws Exception
    {
        session.execute("""
                            steps: |
                                Open https://www.wikipedia.org
                                Select the '${language}' language
                                Type '${searchPhrase}' in the Search input field
                                Click the search button
                                Verify the main heading contains '${searchPhrase}' (bug)
                            """);
    }
}
