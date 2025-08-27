package com.xceptance.neodymium.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import io.qameta.allure.junit5.AllureJunit5;

@Retention(RetentionPolicy.RUNTIME)
@Target(
{
  ElementType.TYPE, ElementType.METHOD
})
@TestTemplate
@ExtendWith(
{
  NeodymiumRunner.class, AllureJunit5.class

})
public @interface NeodymiumTest
{

}
