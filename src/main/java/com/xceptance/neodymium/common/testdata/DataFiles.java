package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Container annotation that allows grouping of multiple {@link DataFile} annotations.
 */
@Retention(RUNTIME)
@Target(
{
  TYPE
})
public @interface DataFiles
{
    /**
     * @return array of {@link DataFile} annotations
     */
    DataFile[] value();
}
