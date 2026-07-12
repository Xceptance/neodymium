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
package org.neodymium.ai.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe event bus that coordinates pipeline and diagnostic event dispatches.
 * Implements re-entrant loop prevention to block listeners from triggering recursive dispatches.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecutionEventBus
{
    /**
     * The thread-safe collection of registered event listeners.
     */
    private final List<ExecutionListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * ThreadLocal flag tracking whether the active thread is currently running a dispatch loop.
     */
    private final ThreadLocal<Boolean> isDispatching = ThreadLocal.withInitial(() -> false);

    /**
     * Constructs a default ExecutionEventBus.
     */
    public ExecutionEventBus()
    {
    }

    /**
     * Registers an event listener subscriber.
     *
     * @param listener the listener to register
     */
    public void registerListener(final ExecutionListener listener)
    {
        if (listener != null && !this.listeners.contains(listener))
        {
            this.listeners.add(listener);
        }
    }

    /**
     * Unregisters an event listener subscriber.
     *
     * @param listener the listener to remove
     */
    public void unregisterListener(final ExecutionListener listener)
    {
        if (listener != null)
        {
            this.listeners.remove(listener);
        }
    }

    /**
     * Dispatches an event to all registered listeners sequentially.
     * Blocks re-entrant dispatches on the same thread by throwing an exception.
     *
     * @param event the event to dispatch
     * @throws ReentrantDispatchException if recursive re-entrant dispatching is triggered by a listener
     */
    public void dispatch(final ExecutionEvent event)
    {
        if (event == null)
        {
            return;
        }

        if (this.isDispatching.get())
        {
            throw new ReentrantDispatchException("Re-entrant event dispatch detected! " 
                + "A listener tried to trigger dispatch of " + event.getClass().getSimpleName() 
                + " recursively during an active dispatch loop.");
        }

        this.isDispatching.set(true);
        try
        {
            for (final ExecutionListener listener : this.listeners)
            {
                listener.onEvent(event);
            }
        }
        finally
        {
            this.isDispatching.set(false);
        }
    }
}
