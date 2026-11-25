/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.server;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.server.session.TestSession;
import org.neodymium.ai.server.verla.model.User;
import org.neodymium.ai.util.StorefrontPatcher;

/**
 * Tests port fallback mechanism, loopback binding, and granular state lifecycle of {@link JavalinTestServer}.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class JavalinTestServerLifecycleTest
{
    /**
     * Tests that creating a second server on an occupied port falls back to an available dynamic port.
     *
     * @throws IOException if server fails to initialize
     */
    @Test
    public void testPortFallbackWhenPortOccupied() throws IOException
    {
        final JavalinTestServer primaryServer = new JavalinTestServer();
        primaryServer.start();

        try
        {
            final int primaryPort = primaryServer.getPort();
            final int primaryHttpsPort = primaryServer.getHttpsPort();

            Assertions.assertTrue(primaryPort > 0, "Primary HTTP port should be valid");
            Assertions.assertTrue(primaryHttpsPort > 0, "Primary HTTPS port should be valid");

            // Attempt to create secondary server requesting the exact same ports
            final JavalinTestServer secondaryServer = new JavalinTestServer(primaryPort, primaryHttpsPort);
            secondaryServer.start();

            try
            {
                final int secondaryPort = secondaryServer.getPort();
                final int secondaryHttpsPort = secondaryServer.getHttpsPort();

                Assertions.assertTrue(secondaryPort > 0, "Secondary HTTP port should be valid");
                Assertions.assertTrue(secondaryHttpsPort > 0, "Secondary HTTPS port should be valid");
                Assertions.assertNotEquals(primaryPort, secondaryPort, "Secondary server should bind to a fallback port");
                Assertions.assertNotEquals(primaryHttpsPort, secondaryHttpsPort, "Secondary server HTTPS should bind to a fallback port");
            }
            finally
            {
                secondaryServer.stop();
            }
        }
        finally
        {
            primaryServer.stop();
        }
    }

    /**
     * Tests that granular reset methods independently reset specific components without wiping others.
     *
     * @throws IOException if server fails to initialize
     */
    @Test
    public void testGranularResets() throws IOException
    {
        final JavalinTestServer server = new JavalinTestServer();
        server.start();

        try
        {
            final TestSession defaultSession = server.getSessionManager().getDefaultSession();

            // Initial state has 1 default user
            Assertions.assertEquals(1, defaultSession.getUsersDb().size());
            Assertions.assertTrue(defaultSession.getUsersDb().containsKey("johndoe@example.com"));

            // Add custom test user
            final User customUser = new User("custom@example.com", "pass123");
            defaultSession.getUsersDb().put(customUser.email, customUser);
            Assertions.assertEquals(2, defaultSession.getUsersDb().size());
            Assertions.assertTrue(defaultSession.getUsersDb().containsKey("custom@example.com"));

            // Resetting inventory or carts does not remove custom user
            server.resetInventory();
            server.resetCarts();
            server.resetOrders();
            Assertions.assertEquals(2, defaultSession.getUsersDb().size());
            Assertions.assertTrue(defaultSession.getUsersDb().containsKey("custom@example.com"));

            // Resetting users explicitly restores only default user
            server.resetUsers();
            Assertions.assertEquals(1, defaultSession.getUsersDb().size());
            Assertions.assertFalse(defaultSession.getUsersDb().containsKey("custom@example.com"));
            Assertions.assertTrue(defaultSession.getUsersDb().containsKey("johndoe@example.com"));

            // Resetting all cleans entire state
            defaultSession.getUsersDb().put(customUser.email, customUser);
            Assertions.assertEquals(2, defaultSession.getUsersDb().size());
            server.resetAll();
            Assertions.assertEquals(1, defaultSession.getUsersDb().size());
            Assertions.assertFalse(defaultSession.getUsersDb().containsKey("custom@example.com"));
        }
        finally
        {
            server.stop();
        }
    }

    /**
     * Tests that the HTTP server binds strictly to a loopback address (127.0.0.1) by default.
     *
     * @throws IOException if server fails to initialize
     */
    @Test
    public void testLoopbackBinding() throws IOException
    {
        final JavalinTestServer server = new JavalinTestServer();
        server.start();

        try
        {
            final ServerConnector connector = server.getHttpConnector();
            Assertions.assertNotNull(connector, "HTTP connector should not be null");
            Assertions.assertEquals("127.0.0.1", connector.getHost(), "HTTP connector host should be 127.0.0.1");
            Assertions.assertTrue(server.getHttpUrl().startsWith("http://127.0.0.1:"), "HTTP URL must use 127.0.0.1 loopback");
        }
        finally
        {
            server.stop();
        }
    }

    /**
     * Tests that StorefrontPatcher resolves an existing shop directory dynamically rather than failing on personal paths.
     */
    @Test
    public void testStorefrontPatcherResolveBaseDir()
    {
        final String resolvedPath = StorefrontPatcher.resolveBaseDir();
        Assertions.assertNotNull(resolvedPath, "StorefrontPatcher baseDir should be resolved");
        final File resolvedDir = new File(resolvedPath);
        Assertions.assertTrue(resolvedDir.exists(), "Resolved baseDir should exist on disk: " + resolvedDir.getAbsolutePath());
        Assertions.assertTrue(resolvedDir.isDirectory(), "Resolved baseDir should be a directory: " + resolvedDir.getAbsolutePath());
    }

    /**
     * Tests that {@link JavalinTestServer#isBindError(Throwable)} traverses deeply nested cause chains
     * without flagging unrelated exceptions.
     */
    @Test
    public void testIsBindErrorDetectsDeeplyNestedBindException()
    {
        final BindException rootBind = new BindException("Cannot assign requested address");
        final IOException middle = new IOException("Connector start failed", rootBind);
        final RuntimeException wrapper = new RuntimeException("Javalin startup aborted", middle);

        Assertions.assertTrue(JavalinTestServer.isBindError(wrapper), "Should detect BindException 3 levels deep");
        Assertions.assertTrue(JavalinTestServer.isBindError(new RuntimeException("Wrapper", new IOException("Failed to bind to 127.0.0.1:8542"))),
            "Should detect nested bind message");
        Assertions.assertFalse(JavalinTestServer.isBindError(new RuntimeException("Wrapper", new IllegalStateException("Something else"))),
            "Should return false for unrelated exceptions");
        Assertions.assertFalse(JavalinTestServer.isBindError(null), "Should safely handle null throwable");
    }

    /**
     * Tests that multiple ephemeral {@link JavalinTestServer} instances can start concurrently
     * without port collisions and bind to distinct HTTP and HTTPS ports.
     *
     * @throws Exception if concurrent startup fails
     */
    @Test
    public void testConcurrentEphemeralServersStartOnDistinctPorts() throws Exception
    {
        final int serverCount = 4;
        final ExecutorService executor = Executors.newFixedThreadPool(serverCount);
        final CountDownLatch readyLatch = new CountDownLatch(serverCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final List<JavalinTestServer> startedServers = new ArrayList<>();

        try
        {
            final List<Callable<JavalinTestServer>> tasks = new ArrayList<>();
            for (int i = 0; i < serverCount; i++)
            {
                tasks.add(() ->
                {
                    final JavalinTestServer server = new JavalinTestServer(0, 0);
                    readyLatch.countDown();
                    Assertions.assertTrue(startLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for concurrent start gate");
                    server.start();
                    return server;
                });
            }

            final List<Future<JavalinTestServer>> futures = new ArrayList<>();
            for (final Callable<JavalinTestServer> task : tasks)
            {
                futures.add(executor.submit(task));
            }
            Assertions.assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Workers did not become ready in time");
            startLatch.countDown();

            final Set<Integer> httpPorts = new HashSet<>();
            final Set<Integer> httpsPorts = new HashSet<>();
            for (final Future<JavalinTestServer> future : futures)
            {
                final JavalinTestServer server = future.get(30, TimeUnit.SECONDS);
                startedServers.add(server);
                Assertions.assertTrue(server.isRunning(), "Server should be running");
                Assertions.assertTrue(server.getPort() > 0, "HTTP port must be assigned");
                Assertions.assertTrue(server.getHttpsPort() > 0, "HTTPS port must be assigned");
                Assertions.assertTrue(httpPorts.add(server.getPort()), "Duplicate HTTP port assigned: " + server.getPort());
                Assertions.assertTrue(httpsPorts.add(server.getHttpsPort()), "Duplicate HTTPS port assigned: " + server.getHttpsPort());
            }

            Assertions.assertEquals(serverCount, httpPorts.size());
            Assertions.assertEquals(serverCount, httpsPorts.size());
        }
        finally
        {
            for (final JavalinTestServer server : startedServers)
            {
                server.stop();
            }
            executor.shutdownNow();
        }
    }
}
