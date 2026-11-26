/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.common.xtc.dto;

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
