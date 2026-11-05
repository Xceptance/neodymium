package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to override the data sets file location. Usually, this file is expected to be named as
 * the test case and to be located in the same package in the resource folder.
 * <p>
 * The file referred to by this annotation needs to reside within the resource path. Provide the full path to the file
 * relative to the resource folder, including the file extension.
 * 
 * @author m.pfotenhauer
 */
@Retention(RUNTIME)
@Target(
{
  TYPE
})
public @interface DataFile
{
    /**
     * get path to test data file
     * 
     * @return
     */
    String value() default "";
}
