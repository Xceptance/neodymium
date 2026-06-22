package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to load a complete folder of datasets.
 * It will collect every supported file inside the folder and read all of them into the test data, 
 * allowing test multiplication over different files.
 */
@Retention(RUNTIME)
@Target(
{
  TYPE
})
@Repeatable(DataFolders.class)
public @interface DataFolder
{
    /**
     * get path to the test data folder
     * 
     * @return folder path relative to the test resource root
     */
    String value() default "";
}
