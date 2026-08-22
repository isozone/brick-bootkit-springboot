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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare a service dependency.
 * <p>
 * Can be used standalone or within {@link PluginService#dependencies()}.
 * <p>
 * Usage example:
 * <pre>
 * &#64;ServiceDependency(
 *     interfaceClass = IUserService.class,
 *     versionRange = "[1.0,2.0)",
 *     optional = false
 * )
 * public class MyService {
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
@Repeatable(ServiceDependencies.class)
public @interface ServiceDependency {

    /**
     * Required service interface class.
     */
    Class<?> interfaceClass();

    /**
     * Version range (semantic versioning).
     * <p>
     * Examples: "[1.0,2.0)", ">=1.0.0", "[1.0,)"
     */
    String versionRange() default "[1.0.0,2.0.0)";

    /**
     * Whether this dependency is optional.
     * <p>
     * If true, the plugin will start even if the dependency is not available.
     */
    boolean optional() default false;

    /**
     * Description of the dependency.
     */
    String description() default "";
}
