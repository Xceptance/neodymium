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
package org.neodymium.ai.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.probe.LocatorProbeResult;
import org.neodymium.ai.model.ContextLevel;

/**
 * Mock implementation of {@link TargetExecutor} used in unit testing to simulate
 * a SUT executor environment, track action calls, and mock locator probing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class MockTargetExecutor implements TargetExecutor
{
    /**
     * The thread-safe list of actions executed through this mock target.
     */
    private final List<Action> executedActions = new CopyOnWriteArrayList<>();

    /**
     * The queue of canned states to be consumed sequentially during captureState calls.
     */
    private final Queue<SutState> stateQueue = new ConcurrentLinkedQueue<>();

    /**
     * The thread-safe list of full-page flags passed into captureState.
     */
    private final List<Boolean> capturedFullPageFlags = new CopyOnWriteArrayList<>();

    /**
     * Canned locator probe results for headless testing.
     */
    private final Map<String, LocatorProbeResult> cannedProbeResults = new ConcurrentHashMap<>();

    /**
     * Flag indicating whether this mock executor reports probing support.
     */
    private boolean probingSupported = true;

    /**
     * The set of supported actions declared by this executor.
     */
    private final Set<ActionDefinition> supportedActions;

    /**
     * Constructs a MockTargetExecutor with standard default supported actions (CLICK, TYPE).
     */
    public MockTargetExecutor()
    {
        this.supportedActions = Set.of(
            new ActionDefinition("CLICK", "Click an element", Collections.emptyMap()),
            new ActionDefinition("TYPE", "Type text into a field", Collections.emptyMap())
        );
    }

    /**
     * Constructs a MockTargetExecutor with a custom set of supported actions.
     *
     * @param supportedActions the specific actions supported by this executor
     */
    public MockTargetExecutor(final Set<ActionDefinition> supportedActions)
    {
        this.supportedActions = supportedActions == null ? Set.of() : Set.copyOf(supportedActions);
    }

    /**
     * Enqueues a canned SUT state to be returned by a subsequent captureState call.
     *
     * @param state the canned state to queue
     */
    public void enqueueState(final SutState state)
    {
        if (state != null)
        {
            this.stateQueue.add(state);
        }
    }

    /**
     * Sets whether this mock executor declares support for locator probing.
     *
     * @param supported whether probing is supported
     */
    public void setProbingSupported(final boolean supported)
    {
        this.probingSupported = supported;
    }

    /**
     * Registers a canned probe result for a specific candidate locator.
     *
     * @param locator candidate locator
     * @param result probe result to return
     */
    public void registerProbeResult(final String locator, final LocatorProbeResult result)
    {
        if (locator != null && result != null)
        {
            this.cannedProbeResults.put(locator, result);
        }
    }

    /**
     * Clears all registered canned probe results.
     */
    public void clearProbeResults()
    {
        this.cannedProbeResults.clear();
    }

    @Override
    public boolean supportsLocatorProbing()
    {
        return this.probingSupported;
    }

    @Override
    public List<LocatorProbeResult> probeLocators(final List<String> candidateLocators, final int maxDepth)
    {
        if (!this.probingSupported)
        {
            return candidateLocators != null
                    ? candidateLocators.stream().map(LocatorProbeResult::unsupported).toList()
                    : Collections.emptyList();
        }
        if (candidateLocators == null || candidateLocators.isEmpty())
        {
            return Collections.emptyList();
        }
        return candidateLocators.stream()
                .map(loc -> this.cannedProbeResults.getOrDefault(loc, LocatorProbeResult.supported(loc, 0, Collections.emptyList())))
                .toList();
    }

    /**
     * Retrieves the list of actions that have been executed on this target.
     *
     * @return the list of executed actions
     */
    public List<Action> getExecutedActions()
    {
        return new ArrayList<>(this.executedActions);
    }

    /**
     * Dequeues the next canned state. Falls back to a default state if the queue is empty.
     *
     * @return the captured SUT state
     * @throws IOException if state capture fails
     */
    @Override
    public SutState captureState(final ContextLevel level) throws IOException
    {
        final SutState next = this.stateQueue.poll();
        if (next != null)
        {
            return next;
        }
        return new MockSutState("<html>default</html>", "default-hash");
    }

    /**
     * Dequeues the next canned state, recording the requested full-page flag.
     *
     * @param level the active context level to capture
     * @param isFullPage true to force full-page screenshot capture
     * @return the captured SUT state
     * @throws IOException if state capture fails
     */
    @Override
    public SutState captureState(final ContextLevel level, final boolean isFullPage) throws IOException
    {
        this.capturedFullPageFlags.add(isFullPage);
        return captureState(level);
    }

    /**
     * Retrieves the list of fullPage flags recorded by captureState calls.
     *
     * @return the list of captured fullPage flags
     */
    public List<Boolean> getCapturedFullPageFlags()
    {
        return new ArrayList<>(this.capturedFullPageFlags);
    }

    /**
     * Records the action in the executed list.
     *
     * @param action the executable action instance
     * @throws IOException if execution fails
     */
    @Override
    public void execute(final Action action) throws IOException
    {
        if (action != null)
        {
            this.executedActions.add(action);
        }
    }

    /**
     * Returns the set of supported ActionDefinitions.
     *
     * @return the set of supported ActionDefinitions
     */
    @Override
    public Set<ActionDefinition> getSupportedActions()
    {
        return this.supportedActions;
    }
}
