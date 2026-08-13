/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import org.junit.jupiter.api.TestTemplate;

/**
 * Annotation to customize or override the playbook file to execute.
 * <p>
 * <b>Path Resolution Rules:</b>
 * <ul>
 *   <li><b>Package-relative:</b> Values without a leading slash (e.g. {@code "my-playbook.yaml"} or {@code "sub/playbook.yaml"})
 *       are resolved relative to the package directory of the test class.</li>
 *   <li><b>Absolute Classpath:</b> Values with a leading slash (e.g. {@code "/playbooks/integration/store.yaml"})
 *       are resolved starting from the classpath root.</li>
 *   <li><b>Default Convention:</b> If value is omitted or empty ({@code @AiPlaybook}), the runner computes a default path
 *       in the test's package directory as {@code <TestClass>_<methodName>.yaml}.</li>
 * </ul>
 * <p>
 * <b>Scoping:</b> When declared on the class level, it sets the default playbook for all test methods in that class.
 * Declaring it on a method overrides any class-level annotation.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@TestTemplate
public @interface AiPlaybook
{
    /**
     * The playbook resource path (e.g. {@code "HomepageTest.yaml"} or {@code "/playbooks/integration/store.yaml"}).
     *
     * @return the playbook resource path
     */
    String value() default "";

    /**
     * The custom base name for the playbook file.
     *
     * @return the playbook base name
     * @deprecated Legacy name override when using programmatic resolution. Use explicit relative or absolute paths in {@link #value()}.
     */
    @Deprecated
    String name() default "";

    /**
     * The name of the test method whose recorded companion JSON file should be loaded during replay.
     * <p>
     * Useful when a separate live/recording method (e.g. {@code testCheckoutLive}) generates the companion JSON file
     * and a replay method (e.g. {@code testCheckoutReplay}) replays it.
     *
     * @return the target test method name for recording resolution
     */
    String recordingMethod() default "";

    /**
     * A custom base name or relative path for the companion recording JSON file.
     * <p>
     * Multi-dimensional dataset and browser profile suffixes will automatically be attached to this base name.
     *
     * @return the custom companion recording file base name or path
     */
    String recordingFileName() default "";

    /**
     * Custom directory path where recorded companion playbook JSON files are saved.
     * <p>
     * Overrides default parent directory resolution.
     * Useful for directing generated companion recordings to build output target folders (e.g. {@code "target/playbooks/integration"}).
     *
     * @return the custom companion recording output directory path
     */
    String recordingDirectory() default "";
}
