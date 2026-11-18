/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.common.retry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark flaky tests that often fail due to the same error but for which there is a hope that they may
 * succeed on retry.
 * <p>
 * Use it as the following: {@code @Retry(maxNumberOfRetries = 4, exceptions = { "HERE" })}
 * </p>
 * <p>
 * It's possible to let the definition of maxNumberOfRetries out, then it will be set to the default value - 3.
 * </p>
 * <p>
 * If you let out the definition of exceptions, test will be retried on every error
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(
{
  ElementType.TYPE, ElementType.METHOD
})
@Inherited
@Repeatable(Retries.class)
@Deprecated
public @interface Retry
{
    int maxNumberOfRetries() default 3;

    String[] exceptions() default
    {
      ""
    };
}
