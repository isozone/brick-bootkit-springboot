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



package com.zqzqq.bootkits.bootstrap.realize;

/**
 * 插件停止检查器. 自主实现判断是否可以卸载
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public interface StopValidator {


    /**
     * 检查是否可以停止卸载。如果检查器抛出异常. 默认插件不可停止/卸载
     * @return 检查结果
     * @since 3.0.0
     */
    Result verify();


    class Result{
        /**
         * 是否可以卸载？
         */
        private final boolean verify;

        /**
* 不可卸载时的提示内容
         */
        private String message;


        public Result(boolean verify) {
            this.verify = verify;
        }

        public Result(boolean verify, String message) {
            this.verify = verify;
            this.message = message;
        }

        /**
         * 可终止
         * @return Result
         * @since 3.0.3
         */
        public static Result ok(){
            return new Result(true);
        }

        /**
         * 禁止停止
         * @return Result
         * @since 3.0.3
         */
        public static Result forbid(){
            return forbid(null);
        }

        /**
         * 禁止停止
         * @param message 禁止停止信息
         * @return Result
         * @since 3.0.3
         */
        public static Result forbid(String message){
            return new Result(false, message);
        }

        public boolean isVerify() {
            return verify;
        }

        public String getMessage() {
            return message;
        }
    }

}

