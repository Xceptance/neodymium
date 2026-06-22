package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Container annotation that allows grouping of multiple {@link DataFolder} annotations.
 */
@Retention(RUNTIME)
@Target(
{
  TYPE
})
public @interface DataFolders
{
    /**
     * @return array of {@link DataFolder} annotations
     */
    DataFolder[] value();
}
