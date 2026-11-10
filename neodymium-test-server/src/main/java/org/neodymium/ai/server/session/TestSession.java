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
package org.neodymium.ai.server.session;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.neodymium.ai.server.auraglance.AuraGlanceState;
import org.neodymium.ai.server.verla.catalog.VerlaCatalog;
import org.neodymium.ai.server.verla.model.Address;
import org.neodymium.ai.server.verla.model.Card;
import org.neodymium.ai.server.verla.model.Cart;
import org.neodymium.ai.server.verla.model.Order;
import org.neodymium.ai.server.verla.model.Product;
import org.neodymium.ai.server.verla.model.User;
import org.neodymium.ai.util.VerlaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the mutable state (users, sessions, carts, orders, inventory, Aura Glance simulation,
 * and headless PWA simulation) for a specific isolated test session.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class TestSession
{
    private static final Logger LOG = LoggerFactory.getLogger(TestSession.class);

    private final String sessionId;
    private final long createdAt;
    private volatile long lastAccessedAt;

    private final AuraGlanceState auraGlanceState = new AuraGlanceState();
    private final Map<String, User> usersDb = new ConcurrentHashMap<>();
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Cart> activeCarts = new ConcurrentHashMap<>();
    private final Map<String, Order> ordersDb = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> productInventory = new ConcurrentHashMap<>();
    private final Random seededRandom = new Random(42L);

    /** Epoch millisecond timestamp at which a headless session finishes its bootstrap handshake, keyed by cart id. */
    private final Map<String, Long> headlessSessionSettleAt = new ConcurrentHashMap<>();

    /** Bootstrap customer id handed out before the session settles; rejected by the BFF endpoints. */
    private final Map<String, String> headlessStaleCustomerId = new ConcurrentHashMap<>();

    /** Customer id that becomes valid once the session has settled. */
    private final Map<String, String> headlessSettledCustomerId = new ConcurrentHashMap<>();

    public TestSession(final String sessionId)
    {
        this.sessionId = sessionId;
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = this.createdAt;
        resetAll();
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public long getLastAccessedAt()
    {
        return lastAccessedAt;
    }

    public void touch()
    {
        this.lastAccessedAt = System.currentTimeMillis();
    }

    public AuraGlanceState getAuraGlanceState()
    {
        return auraGlanceState;
    }

    public Map<String, User> getUsersDb()
    {
        return usersDb;
    }

    public Map<String, String> getActiveSessions()
    {
        return activeSessions;
    }

    public Map<String, Cart> getActiveCarts()
    {
        return activeCarts;
    }

    public Map<String, Order> getOrdersDb()
    {
        return ordersDb;
    }

    public Map<String, Map<String, Integer>> getProductInventory()
    {
        return productInventory;
    }

    public Random getSeededRandom()
    {
        return seededRandom;
    }

    public void resetInventory()
    {
        this.seededRandom.setSeed(42L);
        this.productInventory.clear();
        for (final Product p : VerlaCatalog.getInstance().getProducts())
        {
            if (p.initialStock != null)
            {
                this.productInventory.put(p.id, new ConcurrentHashMap<>(p.initialStock));
            }
        }
        LOG.debug("Test session '{}' inventory reset.", this.sessionId);
    }

    public void resetUsers()
    {
        this.usersDb.clear();
        final User defaultUser = new User("johndoe@example.com", "topsecret");
        defaultUser.addresses.add(new Address("addr-default", "123 Main St", "Boston", "MA", "02108", "US"));
        defaultUser.cards.add(new Card("card-default", "1111222233334100", "Visa", "12/29", "123"));
        this.usersDb.put(defaultUser.email, defaultUser);
        LOG.debug("Test session '{}' users reset to default test user.", this.sessionId);
    }

    public void resetCarts()
    {
        this.activeCarts.clear();
        resetHeadlessSessions();
        LOG.debug("Test session '{}' active carts cleared.", this.sessionId);
    }

    public void resetOrders()
    {
        this.ordersDb.clear();
        LOG.debug("Test session '{}' orders cleared.", this.sessionId);
    }

    public void resetSessions()
    {
        this.activeSessions.clear();
        resetHeadlessSessions();
        LOG.debug("Test session '{}' active sessions cleared.", this.sessionId);
    }

    public void resetHeadlessSessions()
    {
        this.headlessSessionSettleAt.clear();
        this.headlessStaleCustomerId.clear();
        this.headlessSettledCustomerId.clear();
    }

    public long ensureHeadlessSession(final String cartId)
    {
        return this.headlessSessionSettleAt.computeIfAbsent(cartId, id -> {
            final VerlaConfiguration cfg = VerlaConfiguration.getInstance();
            final long settleIn = cfg.isHeadlessSessionRaceEnabled() ? cfg.calculateHeadlessSessionSettleMs() : 0L;
            this.headlessStaleCustomerId.put(id, "guest" + Long.toHexString(ThreadLocalRandom.current().nextLong() & 0xFFFFFFFFFFL));
            this.headlessSettledCustomerId.put(id, "cust" + Long.toHexString(ThreadLocalRandom.current().nextLong() & 0xFFFFFFFFFFL));
            return System.currentTimeMillis() + settleIn;
        });
    }

    public boolean isHeadlessSessionSettled(final String cartId)
    {
        return System.currentTimeMillis() >= ensureHeadlessSession(cartId);
    }

    public String headlessCustomerId(final String cartId)
    {
        ensureHeadlessSession(cartId);
        return isHeadlessSessionSettled(cartId)
               ? this.headlessSettledCustomerId.get(cartId)
               : this.headlessStaleCustomerId.get(cartId);
    }

    public String getHeadlessSettledCustomerId(final String cartId)
    {
        return this.headlessSettledCustomerId.get(cartId);
    }

    public void resetAll()
    {
        this.auraGlanceState.reset();
        resetInventory();
        resetUsers();
        resetCarts();
        resetOrders();
        resetSessions();
        LOG.debug("Test session '{}' full state reset.", this.sessionId);
    }

    public boolean hasUser(final String email)
    {
        return email != null && this.usersDb.containsKey(email);
    }

    public void addUser(final User user)
    {
        if (user != null && user.email != null)
        {
            this.usersDb.put(user.email, user);
        }
    }

    public int getUserCount()
    {
        return this.usersDb.size();
    }

    public int getCartCount()
    {
        return this.activeCarts.size();
    }

    public int getOrderCount()
    {
        return this.ordersDb.size();
    }

    public int getSessionCount()
    {
        return this.activeSessions.size();
    }
}
