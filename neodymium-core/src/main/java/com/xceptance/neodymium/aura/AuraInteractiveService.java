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
package com.xceptance.neodymium.aura;

import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service managing live interactive CDP HUD sessions, theme preferences, and run id tracking.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraInteractiveService
{
    private final AtomicReference<InteractiveConsoleEngine> currentConsoleEngine = new AtomicReference<>(null);
    private final AtomicReference<String> lastProcessedRunId = new AtomicReference<>(null);
    private final AtomicReference<String> activeTheme = new AtomicReference<>("system");
    private final Map<String, Integer> executionIndexMap = new ConcurrentHashMap<>();
    private final AtomicInteger executionIndexCounter = new AtomicInteger(0);

    public AuraInteractiveService()
    {
    }

    public AtomicReference<InteractiveConsoleEngine> getCurrentConsoleEngineReference()
    {
        return currentConsoleEngine;
    }

    public InteractiveConsoleEngine getCurrentConsoleEngine()
    {
        return currentConsoleEngine.get();
    }

    public void setCurrentConsoleEngine(final InteractiveConsoleEngine engine)
    {
        currentConsoleEngine.set(engine);
    }

    public InteractiveConsoleEngine getOrCreateConsoleEngine()
    {
        InteractiveConsoleEngine engine = currentConsoleEngine.get();
        if (engine == null)
        {
            engine = new InteractiveConsoleEngine("initializing");
            currentConsoleEngine.set(engine);
        }
        return engine;
    }

    public AtomicReference<String> getLastProcessedRunIdReference()
    {
        return lastProcessedRunId;
    }

    public String getLastProcessedRunId()
    {
        return lastProcessedRunId.get();
    }

    public void setLastProcessedRunId(final String runId)
    {
        lastProcessedRunId.set(runId);
    }

    public String getActiveTheme()
    {
        return activeTheme.get();
    }

    public void setActiveTheme(final String theme)
    {
        activeTheme.set(theme);
    }

    public int getExecutionIndex(final String executionKey)
    {
        if (executionKey == null || executionKey.isEmpty())
        {
            return 1;
        }
        return executionIndexMap.computeIfAbsent(executionKey, k -> executionIndexCounter.incrementAndGet());
    }

    public void resetExecutionIndexes()
    {
        executionIndexMap.clear();
        executionIndexCounter.set(0);
    }
}

