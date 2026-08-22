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


package com.zqzqq.bootkits.distributed;

/**
 * 测试用共享服务契约，模拟「宿主与执行节点双方 classpath 都可见的接口」。
 * <p>包含一对同名重载方法（{@code getUserInfo}），用于验证执行节点的签名精确匹配，
 * 避免仅按方法名匹配时选错重载。</p>
 */
public interface UserService {

    String getUserName(Long userId);

    UserInfo getUserInfo(Long userId);

    UserInfo getUserInfo(String name);
}