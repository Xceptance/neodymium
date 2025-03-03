package com.xceptance.neodymium.common.testdata;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to limit and override data set execution for an entire class or at the same time for a
 * single method.
 * <p>
 * The <b>value</b> defines the index of the data set that has to be force used for the class or method.
 * <p>
 * Default is 0 which will not have any effect on execution unless there is a {@link SuppressDataSets} annotation
 * involved. In case a class is annotated with {@link SuppressDataSets} and a test method is annotated @DataSet()
 * or @DataSet(0) then it will override suppression and enforce the method to run with <b>all</b> data sets
 * <p>
 * Any number above zero will enforce execution with only that data set. First data set would be equal to 1 and so on.
 * 
 * @author m.kaufmann
 */
@Retention(RUNTIME)
@Target(
{
  TYPE, METHOD
})
@Repeatable(DataSets.class)
public @interface DataSet
{
    /**
     * get data set index
     * 
     * @return
     */
    int value() default 0;

    /**
     * get data set id
     * 
     * @return
     */
    String id() default "";
}
