package com.xceptance.neodymium.util;

/**
 * @deprecated Use {@link org.neodymium.util.AssertionUtils} instead.
 */
@Deprecated
public class AssertionUtils extends org.neodymium.util.AssertionUtils
{
    protected AssertionUtils()
    {
    }

    @FunctionalInterface
    public interface AssertionBlock extends org.neodymium.util.AssertionUtils.AssertionBlock
    {
    }
}
