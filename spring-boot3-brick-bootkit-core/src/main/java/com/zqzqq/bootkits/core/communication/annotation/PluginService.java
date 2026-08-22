/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.communication.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a plugin service class.
 * <p>
 * Usage example:
 * <pre>
 * &#64;PluginService(
 *     version = "1.0.0",
 *     priority = 0,
 *     dependencies = {
 *         &#64;ServiceDependency(
 *             interfaceClass = IUserService.class,
 *             versionRange = "[1.0,2.0)",
 *             optional = false
 *         )
 *     }
 * )
 * public class MyServiceImpl implements IMyService {
 *     // ...
 * }
 * </pre>
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginService {

    /**
     * Service interface.
     * <p>
     * If not specified, the first interface implemented by this class will be used.
     */
    Class<?> interfaceClass() default void.class;

    /**
     * Service version (semantic versioning).
     */
    String version() default "1.0.0";

    /**
     * Service name (human-readable).
     */
    String name() default "";

    /**
     * Description of the service.
     */
    String description() default "";

    /**
     * Priority (lower number = higher priority).
     */
    int priority() default 0;

    /**
     * Whether this is a singleton service.
     */
    boolean singleton() default true;

    /**
     * Whether this service is enabled.
     */
    boolean enabled() default true;

    /**
     * Tags for grouping and filtering.
     */
    String[] tags() default {};

    /**
     * Service dependencies.
     */
    ServiceDependency[] dependencies() default {};
}
