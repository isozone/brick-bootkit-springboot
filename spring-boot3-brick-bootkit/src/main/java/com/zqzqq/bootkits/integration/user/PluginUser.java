/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.integration.user;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * 璇ユ帴接口用于在主程序/插件中进行滆幏获取主程序/插件?Spring 绠＄悊 Bean
 * @author starBlues
 * @version 3.0.0
 */
public interface PluginUser {

    /**
     * 获取 Bean名称
     * @param includeMainBeans 是否包含主程序Bean
     * @return Bean 包装对象
     */
    BeanWrapper<Set<String>> getBeanName(boolean includeMainBeans);

    /**
     * 获取插件中的Bean名称
     * @param pluginId 插件id
     * @return Bean名称集合
     */
    Set<String> getBeanName(String pluginId);


    /**
     * 通过 Bean名称获取 Bean 对象
     * @param name Bean的名称
     * @param includeMainBeans 是否包含主程序Bean
     * @return Bean对象
     */
    BeanWrapper<Object> getBean(String name, boolean includeMainBeans);

    /**
     * 通过 Bean名称获取具体插件中的 Bean 对象
     * @param pluginId 插件id
     * @param name Bean名称
     * @return Object
     */
    Object getBean(String pluginId, String name);


    /**
     * 通过接口获取实现的对象集合
     * @param interfaceClass 接口的类
     * @param includeMainBeans 是否包含主程序Bean
     * @param <T> Bean的类
     * @return Bean包装对象
     */
    <T> BeanWrapper<List<T>> getBeanByInterface(Class<T> interfaceClass, boolean includeMainBeans);

    /**
     * 通过接口获取具体插件中的实现对象集合
     * @param pluginId 插件id
     * @param interfaceClass 接口的类
     * @param <T> Bean的类
     * @return List
     */
    <T> List<T> getBeanByInterface(String pluginId, Class<T> interfaceClass);

    /**
     * 通过注解获取 Bean
     * @param annotationType 注解类型
     * @param includeMainBeans 是否包含主程序Bean
     * @return Bean包装对象
     */
    BeanWrapper<List<Object>> getBeansWithAnnotation(Class<? extends Annotation> annotationType, boolean includeMainBeans);

    /**
     * 通过注解获取具体插件中的 Bean
     * @param pluginId  插件id
     * @param annotationType 注解类型
     * @return 该注解的 Bean 集合
     */
    List<Object> getBeansWithAnnotation(String pluginId, Class<? extends Annotation> annotationType);


}

