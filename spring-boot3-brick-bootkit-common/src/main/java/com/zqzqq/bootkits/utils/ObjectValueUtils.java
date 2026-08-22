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



package com.zqzqq.bootkits.utils;

/**
 * object value convert utils
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public abstract class ObjectValueUtils {

    private ObjectValueUtils(){
    }

    public static String getString(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof CharSequence){
            return ((CharSequence) value).toString();
        }
        return String.valueOf(value);
    }

    public static Integer getInteger(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof Integer){
            return (Integer) value;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    public static Long getLong(Object value){
        if(value == null){
            return null;
        }
        if(value instanceof Long){
            return (Long) value;
        }
        return Long.parseLong(String.valueOf(value));
    }

    public static Double getDouble(Object value) {
        if(value == null){
            return null;
        }
        if(value instanceof Double){
            return (Double) value;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    public static Float getFloat(Object value) {
        if(value == null){
            return null;
        }
        if(value instanceof Float){
            return (Float) value;
        }
        return Float.parseFloat(String.valueOf(value));
    }

    public static Boolean getBoolean(Object value) {
        if(value == null){
            return null;
        }
        if(value instanceof Boolean){
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

}

