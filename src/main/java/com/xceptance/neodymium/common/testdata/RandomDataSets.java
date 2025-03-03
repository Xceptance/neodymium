package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotate a class or method with {@link RandomDataSets} to randomly select data sets for the test execution.
 * <p>
 * The <b>value</b> defines the number of randomly picked data sets. Please pay attention to the fact, that the value
 * should not exceed the number of available data sets, otherwise the exception will be thrown. Note: if the test is
 * also annotated with {@link DataSet} annotations, the random data sets will be selected among the ones mentioned in
 * that annotations
 * <p>
 * Default is 1 which means the test will be executed for the single random data set.
 * <p>
 * Any value below 1 will deactivate this annotation.
 * 
 * @author o.omelianchuk
 */
@Retention(RUNTIME)
@Target(
{
  TYPE, METHOD
})
public @interface RandomDataSets
{
    int value() default 1;
}
