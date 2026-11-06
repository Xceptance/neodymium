package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation to instantiate object with values stemming from the test data.
 * <p>
 * By default, the JSON objects or primitives with a name matching the name of the variable will be parsed to the
 * variable. In case there are no JSON objects that match the variable name found, the JSON objects or names that match
 * the fields of the variable will be used to instantiate the corresponding fields.
 * </p>
 * <p>
 * In case you would like to change the path to the JSON used for the variable instantiation, please pass the JSON path
 * to the corresponding JSON to the <b>value</b> of the annotation
 * </p>
 */
@Retention(RUNTIME)
@Target(
{
  ElementType.FIELD
})
public @interface DataItem
{
    /**
     * get JSON path to data item object
     * 
     * @return
     */
    String value() default "";
}
