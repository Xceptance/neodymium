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
package org.neodymium.common.xtc.dto;

// Data Transfer Object for authentication responses from the XTC API.
public class AuthResponse
{
    private final String access_token;

    private final String issued_token_type;

    private final String scope;

    private final String token_type;

    private final Integer expires_in;

    public AuthResponse(String accessToken, String issuedTokenType, String scope, String tokenType, Integer expiresIn)
    {
        this.access_token = accessToken;
        this.issued_token_type = issuedTokenType;
        this.scope = scope;
        this.token_type = tokenType;
        this.expires_in = expiresIn;
    }

    public String getAccessToken()
    {
        return access_token;
    }

    public String getIssuedTokenType()
    {
        return issued_token_type;
    }

    public String getScope()
    {
        return scope;
    }

    public String getTokenType()
    {
        return token_type;
    }

    public Integer getExpiresIn()
    {
        return expires_in;
    }
}
