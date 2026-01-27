/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.bootstrap.coexist;

import com.zqzqq.bootkits.bootstrap.annotation.ResolveClassLoader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Coexist模式下解决调用方法时, 非本插件的ClassLoader
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
@Aspect
public class CoexistResolveClassLoaderAspect {

    @Pointcut("@annotation(com.zqzqq.bootkits.bootstrap.annotation.ResolveClassLoader)")
    public void test() {

    }

    @Around("@annotation(resolveClassLoader)")
    public Object around(ProceedingJoinPoint pjp, ResolveClassLoader resolveClassLoader) throws Throwable{
        Thread thread = Thread.currentThread();
        ClassLoader oldClassLoader = thread.getContextClassLoader();
        try {
            Object target = pjp.getTarget();
            thread.setContextClassLoader(target.getClass().getClassLoader());
            return pjp.proceed();
        } finally {
            thread.setContextClassLoader(oldClassLoader);
        }
    }

}