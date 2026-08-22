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



package com.zqzqq.bootkits.spring;

import lombok.Data;

import java.util.Set;

/**
 * 閹绘帊娆㈠☉鎿冨幖椤曠晲eb闁汇劌瀚伴崢銈囩磾?
 * @author starBlues
 * @version 3.0.0
 */
@Data
public class WebConfig {

    private boolean enable = false;
    private Set<String> resourceLocations = null;
    
    public boolean isEnable() {
        return enable;
    }
    
    public Set<String> getResourceLocations() {
        return resourceLocations;
    }
}

