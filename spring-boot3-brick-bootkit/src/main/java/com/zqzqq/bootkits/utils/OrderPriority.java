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

package com.zqzqq.bootkits.utils;

/**
 * 椤哄簭浼樺厛绾?
 *
 * @author starBlues
 * @since 1.0.0
 * @version 1.0.0
 */
public class OrderPriority {
    /**
     * 楂樹紭鍏堢骇
     */
    private static final Integer HIGH_PRIORITY = 1000;

    /**
     * 中紭鍏堢骇
     */
    private static final Integer MIDDLE_PRIORITY = 0;

    /**
     * 浣庝紭鍏堢骇
     */
    private static final Integer LOW_PRIORITY = -1000;


    private Integer priority;


    private OrderPriority(Integer priority){
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    /**
     * 得到浣庝紭鍏堢骇
     * @return OrderPriority
     */
    public static OrderPriority getLowPriority(){
        return new OrderPriority(LOW_PRIORITY);
    }


    /**
     * 得到中紭鍏堢骇
     * @return OrderPriority
     */
    public static OrderPriority getMiddlePriority(){
        return new OrderPriority(MIDDLE_PRIORITY);
    }


    /**
     * 得到楂樹紭鍏堢骇
     * @return OrderPriority
     */
    public static OrderPriority getHighPriority(){
        return new OrderPriority(HIGH_PRIORITY);
    }


    /**
     * 鍗囦紭鍏堢骇.链楂樺彧鑳藉崌鍒版渶楂樹紭鍏堢骇鍒備篃就是1000
     * @param number 鍗囩骇鏁伴噺
     * @return OrderPriority
     */
    public OrderPriority up(Integer number){
        if(number == null){
            return this;
        }
        if(HIGH_PRIORITY.equals(this.priority) || (this.priority + number) > HIGH_PRIORITY){
            this.priority = HIGH_PRIORITY;
            return this;
        } else {
            this.priority = this.priority + number;
            return this;
        }
    }

    /**
     * 闄嶄紭鍏堢骇.链浣庡彧鑳介檷鍒版渶浣庝紭鍏堢骇鍒備篃就是-1000
     * @param number 闄嶇骇鏁伴噺
     * @return OrderPriority
     */
    public OrderPriority down(Integer number){
        if(number == null){
            return this;
        }
        if(MIDDLE_PRIORITY.equals(this.priority) || (this.priority - number) < MIDDLE_PRIORITY){
            this.priority = MIDDLE_PRIORITY;
            return this;
        } else {
            this.priority = this.priority - number;
            return this;
        }
    }



}

