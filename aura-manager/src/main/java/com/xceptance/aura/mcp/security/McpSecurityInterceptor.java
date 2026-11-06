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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Interceptor for enforcing Bearer token authentication and security policies on the MCP endpoint.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Component
public class McpSecurityInterceptor implements HandlerInterceptor
{
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuraMcpProperties properties;

    public McpSecurityInterceptor(final AuraMcpProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
        throws Exception
    {
        if (!properties.isEnabled())
        {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "MCP Server is disabled");
            return false;
        }

        final String configuredToken = properties.getAuth() != null ? properties.getAuth().getToken() : null;

        // If no token is configured, allow only loopback/local access
        if (configuredToken == null || configuredToken.isBlank())
        {
            if (!isLoopbackRequest(request))
            {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"Remote MCP access requires an authentication token\"}");
                return false;
            }
            return true;
        }

        final String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX))
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Bearer realm=\"Aura MCP\"");
            response.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return false;
        }

        final String presentedToken = authHeader.substring(BEARER_PREFIX.length()).trim();
        final byte[] expectedBytes = configuredToken.getBytes(StandardCharsets.UTF_8);
        final byte[] actualBytes = presentedToken.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expectedBytes, actualBytes))
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Invalid MCP authentication token\"}");
            return false;
        }

        return true;
    }

    private boolean isLoopbackRequest(final HttpServletRequest request)
    {
        final String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank())
        {
            return false;
        }

        if ("127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr) || "localhost".equalsIgnoreCase(remoteAddr))
        {
            return true;
        }

        try
        {
            return InetAddress.getByName(remoteAddr).isLoopbackAddress();
        }
        catch (final Exception e)
        {
            return false;
        }
    }
}
