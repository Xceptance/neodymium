package com.xceptance.neodymium.common.xtc.dto;

/**
 * @deprecated Use {@link org.neodymium.common.xtc.dto.AuthResponse} instead.
 */
@Deprecated
public class AuthResponse extends org.neodymium.common.xtc.dto.AuthResponse
{
    public AuthResponse(java.lang.String accessToken, java.lang.String issuedTokenType, java.lang.String scope, java.lang.String tokenType, java.lang.Integer expiresIn)
    {
        super(accessToken, issuedTokenType, scope, tokenType, expiresIn);
    }
}
