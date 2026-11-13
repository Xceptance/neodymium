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
package com.xceptance.aura.mcp.security;

import com.xceptance.aura.mcp.config.AuraMcpProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link McpSecurityInterceptor} authentication and access policies.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class McpSecurityInterceptorTest
{
    private AuraMcpProperties properties;
    private McpSecurityInterceptor interceptor;

    @BeforeEach
    public void setUp()
    {
        properties = new AuraMcpProperties();
        properties.setEnabled(true);
        interceptor = new McpSecurityInterceptor(properties);
    }

    @Test
    public void testPreHandleWhenDisabled() throws Exception
    {
        properties.setEnabled(false);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertFalse(allowed, "Disabled MCP server must reject incoming requests");
        Assertions.assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
    }

    @Test
    public void testPreHandleWhenNoTokenConfigured() throws Exception
    {
        properties.getAuth().setToken(null);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertTrue(allowed, "Loopback requests must be allowed when no auth token is configured");
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void testPreHandleWhenNoTokenConfiguredAndRemoteClientRejected() throws Exception
    {
        properties.getAuth().setToken(null);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.25");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertFalse(allowed, "Remote requests without auth token configured must be rejected with 403 Forbidden");
        Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("Remote MCP access requires an authentication token"));
    }

    @Test
    public void testPreHandleWhenTokenConfiguredButHeaderMissing() throws Exception
    {
        properties.getAuth().setToken("secret-token-123");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertFalse(allowed, "Requests without auth header must be rejected");
        Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        Assertions.assertEquals("Bearer realm=\"Aura MCP\"", response.getHeader("WWW-Authenticate"));
        Assertions.assertTrue(response.getContentAsString().contains("Missing or invalid Authorization header"));
    }

    @Test
    public void testPreHandleWhenTokenConfiguredButHeaderNotBearer() throws Exception
    {
        properties.getAuth().setToken("secret-token-123");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertFalse(allowed, "Non-Bearer auth headers must be rejected");
        Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    public void testPreHandleWhenTokenInvalid() throws Exception
    {
        properties.getAuth().setToken("secret-token-123");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer wrong-token-xyz");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertFalse(allowed, "Requests with mismatched token must be rejected with 403 Forbidden");
        Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("Invalid MCP authentication token"));
    }

    @Test
    public void testPreHandleWhenTokenValid() throws Exception
    {
        properties.getAuth().setToken("secret-token-123");
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer secret-token-123");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final boolean allowed = interceptor.preHandle(request, response, new Object());

        Assertions.assertTrue(allowed, "Requests with valid Bearer token must be permitted");
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }
}
