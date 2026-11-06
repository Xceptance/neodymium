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
