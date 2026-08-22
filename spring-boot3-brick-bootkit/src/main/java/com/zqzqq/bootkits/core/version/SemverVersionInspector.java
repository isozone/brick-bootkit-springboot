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



package com.zqzqq.bootkits.core.version;

import com.github.zafarkhaja.semver.Version;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.utils.ObjectUtils;

/**
 * Semver版本检查器实现
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public class SemverVersionInspector implements VersionInspector{

    @Override
    public void check(String version) throws PluginException {
        try {
            Version.valueOf(version);
        } catch (Throwable e){
            String message = e.toString();
            if(ObjectUtils.isEmpty(message)){
                message = "";
            } else {
                message = ": " + message;
            }
            throw new PluginException("版本[" + version + "]无效" + message);
        }
    }

    @Override
    public int compareTo(String version1, String version2) {
        check(version1);
        check(version2);

        Version v1 = Version.valueOf(version1);
        Version v2 = Version.valueOf(version2);
        return v1.compareTo(v2);
    }

}

