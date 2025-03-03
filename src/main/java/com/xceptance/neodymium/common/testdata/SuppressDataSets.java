package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to suppress automatic test case multiplication for
 * <a href="https://github.com/Xceptance/neodymium-library/wiki/Test-data-provider">data sets and test data</a>.
 * 
 * @author m.kaufmann
 * @see DataSet
 */
@Retention(RUNTIME)
@Target(
    {
        TYPE, METHOD
    })
public @interface SuppressDataSets
{

}
