/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures post-action semantic outcome verification at the class or method level.
 * When multiple values are specified (e.g. {@code @AiOutcomeVerification({false, true})}),
 * the runner executes the test across both verification variations.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AiOutcomeVerification
{
    /**
     * Whether post-action semantic outcome verification is enabled for the test execution.
     * Empty by default so annotating a class with {@code @AiOutcomeVerification(failOnError = false)}
     * sets the failure policy without forcing outcome verification to be enabled on all methods.
     *
     * @return boolean flag(s) indicating if outcome verification is active
     */
    boolean[] value() default {};

    /**
     * Whether outcome verification failures should fail the test execution.
     * Defaults to {@code false}.
     *
     * @return boolean flag indicating if verification failure throws an exception
     */
    boolean failOnError() default false;

    /**
     * Alias for {@link #failOnError()}.
     *
     * @return boolean flag indicating if verification failure throws an exception
     */
    boolean onError() default false;
}
