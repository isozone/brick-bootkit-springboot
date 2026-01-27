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

package com.zqzqq.bootkits.spring;

/**
 * 浠ｇ悊宸ュ巶
 * @author starBlues
 * @version 3.0.0
 */
public interface ProxyFactory {

    /**
     * 获取浠ｇ悊类
     * @param interfacesClass 闇浠ｇ悊鐨勬帴可
     * @param <T> 浠ｇ悊实现鐨勬硾鍨?
     * @return 浠ｇ悊实现
     */
    <T> T getObject(Class<T> interfacesClass);

}

