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


package com.zqzqq.bootkits.loader.utils;

/**
 * 閸欘垶鍣撮弨鎹愮カ濠ф劖甯撮崣?
 *
 * @author starBlues
 * @since 3.1.1
 * @version 4.0.0
 */
public interface Release {

    /**
     * 释放资源
     * @throws Exception 释放异常
     */
    default void release() throws Exception{}

}