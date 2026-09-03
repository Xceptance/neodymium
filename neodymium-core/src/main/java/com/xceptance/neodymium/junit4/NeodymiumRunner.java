package com.xceptance.neodymium.junit4;

import org.junit.runners.model.InitializationError;

/**
 * @deprecated Use {@link org.neodymium.junit4.NeodymiumRunner} instead.
 */
@Deprecated
public class NeodymiumRunner extends org.neodymium.junit4.NeodymiumRunner
{
    public NeodymiumRunner(Class<?> clazz) throws InitializationError
    {
        super(clazz);
    }
}
