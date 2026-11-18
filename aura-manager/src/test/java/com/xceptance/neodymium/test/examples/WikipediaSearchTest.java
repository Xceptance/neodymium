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
package com.xceptance.neodymium.test.examples;

import org.junit.jupiter.api.Disabled;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import com.xceptance.neodymium.common.browser.Browser;

/**
 * JUnit 5 test runner for the Wikipedia Search example using Neodymium AI v2 framework.
 *
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@NeodymiumAiTest
@Disabled("Requires live LLM API key and external internet access; run via ai-live-tests profile")
public final class WikipediaSearchTest
{
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook("tests/examples/wikipedia_search.yml")
    public final void executeWikipediaSearch()
    {
    }
}
