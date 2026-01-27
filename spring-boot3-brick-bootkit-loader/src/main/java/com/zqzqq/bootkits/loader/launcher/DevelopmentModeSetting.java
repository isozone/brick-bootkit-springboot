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

package com.zqzqq.bootkits.loader.launcher;

import com.zqzqq.bootkits.loader.DevelopmentMode;

/**
 * DevelopmentMode 设置类
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
public class DevelopmentModeSetting {

    private static String developmentMode;

    static void setDevelopmentMode(String developmentMode) {
        DevelopmentModeSetting.developmentMode = checkModeKey(developmentMode);
    }

    public static boolean isolation(){
        return DevelopmentMode.ISOLATION.equalsIgnoreCase(developmentMode);
    }

    public static boolean coexist(){
        return DevelopmentMode.COEXIST.equalsIgnoreCase(developmentMode);
    }

    public static String getDevelopmentMode(){
        return developmentMode;
    }

    public static IllegalStateException getUnknownModeException(){
        return getUnknownModeException(null);
    }

    public static IllegalStateException getUnknownModeException(String developmentMode){
        if(developmentMode == null || "".equals(developmentMode)){
            developmentMode = DevelopmentModeSetting.developmentMode;
        }
        return new IllegalStateException("不支持开发模式" + developmentMode);
    }

    private static String checkModeKey(String developmentMode){
        if(developmentMode == null || "".equals(developmentMode)){
            throw new RuntimeException("developmentMode 设置不能为空");
        }
        if(DevelopmentMode.ISOLATION.equalsIgnoreCase(developmentMode)){
            return developmentMode;
        } else if(DevelopmentMode.COEXIST.equalsIgnoreCase(developmentMode)){
            return developmentMode;
        } else {
            throw getUnknownModeException(developmentMode);
        }
    }

}